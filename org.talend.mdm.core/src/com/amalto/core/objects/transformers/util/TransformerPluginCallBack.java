package com.amalto.core.objects.transformers.util;

import com.amalto.core.util.XtentisException;


public interface TransformerPluginCallBack {
	
	public void contentIsReady( TransformerPluginContext pluginContext) throws XtentisException;
	
}
