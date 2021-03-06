/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.objects.cext;

import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.cext.CExtNodes.ToSulongNode;
import com.oracle.graal.python.builtins.objects.cext.NativeWrappers.PythonClassNativeWrapper;
import com.oracle.graal.python.builtins.objects.cext.PyBufferProcsWrapperMRFactory.GetBufferProcsNodeGen;
import com.oracle.graal.python.builtins.objects.type.PythonAbstractClass;
import com.oracle.graal.python.builtins.objects.type.PythonManagedClass;
import com.oracle.graal.python.nodes.PNodeWithContext;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = PyBufferProcsWrapper.class)
public class PyBufferProcsWrapperMR {

    @Resolve(message = "READ")
    abstract static class ReadNode extends Node {
        @Child private GetBufferProcsNode getBufferProcsNode;

        public Object access(PyBufferProcsWrapper object, String key) {
            if (getBufferProcsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getBufferProcsNode = insert(GetBufferProcsNodeGen.create());
            }
            return getBufferProcsNode.execute(object.getPythonClass(), key);
        }
    }

    abstract static class GetBufferProcsNode extends PNodeWithContext {
        @Child private ToSulongNode toSulongNode;

        public abstract Object execute(PythonAbstractClass clazz, String key);

        @Specialization
        Object doManagedClass(PythonManagedClass clazz, String key) {
            // translate key to attribute name
            PythonClassNativeWrapper nativeWrapper = clazz.getNativeWrapper();

            // Since this MR is directly called from native, there must be a native wrapper.
            assert nativeWrapper != null;

            Object result;
            switch (key) {
                case "bf_getbuffer":
                    result = nativeWrapper.getGetBufferProc();
                    break;
                case "bf_releasebuffer":
                    // TODO
                    result = nativeWrapper.getReleaseBufferProc();
                    break;
                default:
                    // TODO extend list
                    throw UnknownIdentifierException.raise(key);
            }
            // do not wrap result if exists since this is directly a native object
            // use NO_VALUE for NULL
            return result == null ? getToSulongNode().execute(PNone.NO_VALUE) : result;
        }

        @Specialization
        Object doNativeClass(@SuppressWarnings("unused") PythonNativeClass clazz, @SuppressWarnings("unused") String key) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("native member access to native class via interop");
        }

        private ToSulongNode getToSulongNode() {
            if (toSulongNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toSulongNode = insert(ToSulongNode.create());
            }
            return toSulongNode;
        }

    }
}
