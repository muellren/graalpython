/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.nodes.util;

import static com.oracle.graal.python.nodes.SpecialMethodNames.__INT__;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.TypeError;

import com.oracle.graal.python.builtins.modules.MathGuards;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.nodes.PNodeWithContext;
import com.oracle.graal.python.nodes.call.special.LookupAndCallUnaryNode;
import com.oracle.graal.python.nodes.truffle.PythonArithmeticTypes;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;

@TypeSystemReference(PythonArithmeticTypes.class)
@ImportStatic(MathGuards.class)
public abstract class CastToJavaLongNode extends PNodeWithContext {
    private final boolean lossy;
    @Node.Child private LookupAndCallUnaryNode callIntNode;

    protected CastToJavaLongNode(boolean lossy) {
        this.lossy = lossy;
    }

    public abstract long execute(Object x);

    public static CastToJavaLongNode createExact() {
        return CastToJavaLongNodeGen.create(false);
    }

    public static CastToJavaLongNode createLossy() {
        return CastToJavaLongNodeGen.create(true);
    }

    @Specialization
    public long toLong(long x) {
        return x;
    }

    @Specialization
    public long toLong(PInt x) {
        return toLongInternal(x);
    }

    private long toLongInternal(PInt x) {
        if (lossy) {
            return x.longValue();
        }
        return x.longValueExact();
    }

    @Specialization(guards = "!isNumber(x)")
    public long toLong(Object x) {
        if (callIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callIntNode = insert(LookupAndCallUnaryNode.create(__INT__));
        }

        Object result = callIntNode.executeObject(x);
        if (result == PNone.NO_VALUE) {
            throw raise(TypeError, "must be numeric, not %p", x);
        }
        if (result instanceof PInt) {
            return toLongInternal((PInt) result);
        }
        if (result instanceof Integer) {
            return ((Integer) result).longValue();
        }
        if (result instanceof Long) {
            return (long) result;
        }
        throw raise(TypeError, "%p.__int__ returned a non long (type %p)", x, result);
    }
}
