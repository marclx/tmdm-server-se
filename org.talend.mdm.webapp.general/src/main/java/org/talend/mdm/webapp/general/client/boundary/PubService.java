package org.talend.mdm.webapp.general.client.boundary;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.general.client.General;
import org.talend.mdm.webapp.general.client.GeneralServiceAsync;
import org.talend.mdm.webapp.general.client.layout.BrandingBar;
import org.talend.mdm.webapp.general.model.ProductInfo;

import com.extjs.gxt.ui.client.Registry;

public class PubService {

    public native static void registerLanguageService()/*-{
		$wnd.getLanguage = function() {
			return @org.talend.mdm.webapp.base.client.util.UrlUtil::getLanguage()();
		};
    }-*/;

    public native static void registerUpdateProductInfo()/*-{
		$wnd.refreshProductInfo = function() {
			@org.talend.mdm.webapp.general.client.boundary.PubService::updateProductInfo()();
		};
    }-*/;

    static void updateProductInfo() {
        GeneralServiceAsync service = (GeneralServiceAsync) Registry.get(General.OVERALL_SERVICE);
        service.getProductInfo(new SessionAwareAsyncCallback<ProductInfo>() {

            @Override
            public void onSuccess(ProductInfo info) {
                BrandingBar.getInstance().setProductInfo(info);
            }
        });
    }
}
