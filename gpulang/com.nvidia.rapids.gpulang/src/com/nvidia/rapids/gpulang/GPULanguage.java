package com.nvidia.rapids.gpulang;

import com.nvidia.rapids.gpulang.GPULanguage.Context;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;


@TruffleLanguage.Registration(id = GPULanguage.ID, name = "GPU", version="0.1", characterMimeTypes = GPULanguage.MIME_TYPE, internal=false)
public class GPULanguage extends TruffleLanguage<Context> {

    public static final String ID = "gpu";
    public static final String MIME_TYPE = "application/x-gpu";

    static class Context {
        Env env;
        Context(Env env) { this.env = env;}
    }

    @Override
    protected Context createContext(Env env) {
        System.out.println("Create Context for GPULanguage");
        return new Context(env);
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        return null;
    }
}
