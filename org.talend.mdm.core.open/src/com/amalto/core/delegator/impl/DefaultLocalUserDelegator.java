package com.amalto.core.delegator.impl;

import com.amalto.core.delegator.ILocalUser;
import com.amalto.core.util.XtentisException;

public class DefaultLocalUserDelegator extends ILocalUser {

    private static ILocalUser iUser = new DefaultLocalUserDelegator();

    private String userXML;

    @Override
    public ILocalUser getILocalUser() throws XtentisException {
        return iUser;
    }

    /**
     * The User in XML form as stored in the DB
     * 
     * @return The user in the DB XML form
     */
    @Override
    public String getUserXML() {
        return userXML;
    }

    @Override
    public void setUserXML(String userXML) {
        this.userXML = userXML;
    }
}
