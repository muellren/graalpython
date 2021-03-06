/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.graal.python.nodes.attributes;

import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.cext.CExtNodes.GetNativeDictNode;
import com.oracle.graal.python.builtins.objects.cext.CExtNodes.GetObjectDictNode;
import com.oracle.graal.python.builtins.objects.cext.CExtNodes.GetTypeMemberNode;
import com.oracle.graal.python.builtins.objects.cext.NativeMemberNames;
import com.oracle.graal.python.builtins.objects.cext.PythonAbstractNativeObject;
import com.oracle.graal.python.builtins.objects.cext.PythonNativeClass;
import com.oracle.graal.python.builtins.objects.cext.PythonNativeObject;
import com.oracle.graal.python.builtins.objects.common.HashingStorage;
import com.oracle.graal.python.builtins.objects.common.HashingStorageNodes;
import com.oracle.graal.python.builtins.objects.common.PHashingCollection;
import com.oracle.graal.python.builtins.objects.object.PythonObject;
import com.oracle.graal.python.nodes.PGuards;
import com.oracle.graal.python.nodes.interop.PForeignToPTypeNode;
import com.oracle.graal.python.runtime.PythonOptions;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;

@ImportStatic({PGuards.class, PythonOptions.class, NativeMemberNames.class})
public abstract class ReadAttributeFromObjectNode extends ObjectAttributeNode {

    protected final boolean forceType;

    public ReadAttributeFromObjectNode(boolean forceType) {
        this.forceType = forceType;
    }

    public static ReadAttributeFromObjectNode create() {
        return ReadAttributeFromObjectNodeGen.create(false);
    }

    public static ReadAttributeFromObjectNode createForceType() {
        return ReadAttributeFromObjectNodeGen.create(true);
    }

    public abstract Object execute(Object object, Object key);

    // read from the DynamicObject store
    @Specialization(guards = {
                    "object == cachedObject"
    }, assumptions = {
                    "singleContextAssumption",
                    "dictUnsetOrSameAsStorageAssumption"
    })
    protected Object readFromDynamicStorageCached(PythonObject object, Object key,
                    @SuppressWarnings("unused") @Cached("object") PythonObject cachedObject,
                    @SuppressWarnings("unused") @Cached("singleContextAssumption()") Assumption singleContextAssumption,
                    @SuppressWarnings("unused") @Cached("cachedObject.getDictUnsetOrSameAsStorageAssumption()") Assumption dictUnsetOrSameAsStorageAssumption,
                    @Cached("create()") ReadAttributeFromDynamicObjectNode readAttributeFromDynamicObjectNode) {
        return readAttributeFromDynamicObjectNode.execute(object.getStorage(), key);
    }

    @Specialization(guards = {
                    "isDictUnsetOrSameAsStorage(object) || isHiddenKey(key)"
    }, replaces = "readFromDynamicStorageCached")
    protected Object readFromDynamicStorage(PythonObject object, Object key,
                    @Cached("create()") ReadAttributeFromDynamicObjectNode readAttributeFromDynamicObjectNode) {
        return readAttributeFromDynamicObjectNode.execute(object.getStorage(), key);
    }

    // read from the Dict
    @Specialization(guards = {
                    "object == cachedObject",
                    "!dictUnsetOrSameAsStorageAssumption.isValid()",
                    "!isHiddenKey(key)"
    }, assumptions = {
                    "singleContextAssumption"
    })
    protected Object readFromDictCached(PythonObject object, Object key,
                    @SuppressWarnings("unused") @Cached("object") PythonObject cachedObject,
                    @SuppressWarnings("unused") @Cached("singleContextAssumption()") Assumption singleContextAssumption,
                    @SuppressWarnings("unused") @Cached("cachedObject.getDictUnsetOrSameAsStorageAssumption()") Assumption dictUnsetOrSameAsStorageAssumption,
                    @Cached("create()") HashingStorageNodes.GetItemNode getItemNode) {
        Object value = getItemNode.execute(getDictStorage(object.getDict()), key);
        if (value == null) {
            return PNone.NO_VALUE;
        } else {
            return value;
        }
    }

    @Specialization(guards = {
                    "!isHiddenKey(key)",
                    "!isDictUnsetOrSameAsStorage(object)"
    }, replaces = "readFromDictCached")
    protected Object readFromDict(PythonObject object, Object key,
                    @Cached("create()") HashingStorageNodes.GetItemNode getItemNode) {
        Object value = getItemNode.execute(getDictStorage(object.getDict()), key);
        if (value == null) {
            return PNone.NO_VALUE;
        } else {
            return value;
        }
    }

    // foreign Object
    protected Node createReadMessageNode() {
        return Message.READ.createNode();
    }

    @Specialization(guards = {"!forceType", "!isHiddenKey(key)"})
    protected Object readNativeObject(PythonNativeObject object, Object key,
                    @Cached("create()") GetObjectDictNode getNativeDict,
                    @Cached("create()") HashingStorageNodes.GetItemNode getItemNode) {
        return readNative(object, key, getNativeDict, getItemNode);
    }

    @Specialization(guards = {"forceType", "!isHiddenKey(key)"})
    protected Object readNativeClass(PythonNativeClass object, Object key,
                    @Cached("create(TP_DICT)") GetTypeMemberNode getNativeDict,
                    @Cached("create()") HashingStorageNodes.GetItemNode getItemNode) {
        return readNative(object, key, getNativeDict, getItemNode);
    }

    private Object readNative(Object object, Object key, GetNativeDictNode getNativeDict, HashingStorageNodes.GetItemNode getItemNode) {
        Object d = getNativeDict.execute(object);
        Object value = null;
        if (d instanceof PHashingCollection) {
            value = getItemNode.execute(getDictStorage((PHashingCollection) d), key);
        }
        if (value == null) {
            return PNone.NO_VALUE;
        } else {
            return value;
        }
    }

    @Specialization(guards = "isForeignObject(object)")
    protected Object readForeign(TruffleObject object, Object key,
                    @Cached("create()") PForeignToPTypeNode fromForeign,
                    @Cached("createReadMessageNode()") Node readNode) {
        try {
            return fromForeign.executeConvert(ForeignAccess.sendRead(readNode, object, attrKey(key)));
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            return PNone.NO_VALUE;
        }
    }

    // not a Python or Foreign Object
    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPythonObject(object)", "!isForeignObject(object)"})
    protected PNone readUnboxed(Object object, Object key) {
        return PNone.NO_VALUE;
    }

    public static Object doSlowPath(Object object, Object key) {
        return doSlowPath(object, key, false);
    }

    @TruffleBoundary
    public static Object doSlowPath(Object object, Object key, boolean forceType) {
        if (object instanceof PythonObject) {
            PythonObject po = (PythonObject) object;
            if (ObjectAttributeNode.isDictUnsetOrSameAsStorage(po)) {
                return ReadAttributeFromDynamicObjectNode.doSlowPath(po.getStorage(), key);
            } else {
                HashingStorage dictStorage = po.getDict().getDictStorage();
                Object value = dictStorage.getItem(key, HashingStorage.getSlowPathEquivalence(key));
                if (value == null) {
                    return PNone.NO_VALUE;
                } else {
                    return value;
                }
            }
        } else if (object instanceof PythonAbstractNativeObject) {
            Object d = forceType ? GetTypeMemberNode.doSlowPath(object, NativeMemberNames.TP_DICT) : GetObjectDictNode.doSlowPath(object);
            Object value = null;
            if (d instanceof PHashingCollection) {
                HashingStorage dictStorage = ((PHashingCollection) d).getDictStorage();
                value = dictStorage.getItem(key, HashingStorage.getSlowPathEquivalence(key));
            }
            if (value == null) {
                return PNone.NO_VALUE;
            } else {
                return value;
            }

        }
        return null;

    }
}
