// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.base.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import junit.framework.TestCase;

import org.talend.mdm.commmon.util.core.EDBType;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.datamodel.management.DataModelID;
import org.talend.mdm.webapp.base.client.model.DataTypeConstants;
import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;
import org.talend.mdm.webapp.base.client.util.MultilanguageMessageParser;
import org.talend.mdm.webapp.base.shared.EntityModel;
import org.talend.mdm.webapp.base.shared.SimpleTypeModel;
import org.talend.mdm.webapp.base.shared.TypeModel;
import org.w3c.dom.Element;

import com.amalto.core.webservice.WSStringPredicate;
import com.amalto.core.webservice.WSWhereCondition;
import com.amalto.core.webservice.WSWhereItem;
import com.amalto.core.webservice.WSWhereOperator;
import com.amalto.webapp.core.util.Util;

@SuppressWarnings("nls")
public class ForeignKeyHelperTest extends TestCase {

    // TODO Refactor FK handler, also this JUnit case
    public void testGetForeignKeyHolder() throws Exception {

        TypeModel model = new SimpleTypeModel("family", DataTypeConstants.STRING); //$NON-NLS-1$
        model.setForeignkey("ProductFamily/Id"); //$NON-NLS-1$
        List<String> foreignKeyInfos = new ArrayList<String>();
        foreignKeyInfos.add("ProductFamily/Name"); //$NON-NLS-1$
        model.setForeignKeyInfo(foreignKeyInfos);
        model.setRetrieveFKinfos(true);
        model.setXpath("Product/Family"); //$NON-NLS-1$
        boolean ifFKFilter = false;
        String value = "Hats"; //$NON-NLS-1$
        String xml = null;
        String dataCluster = "Product";
        String currentXpath = "Product/Family";

        InputStream stream = getClass().getResourceAsStream("product.xsd");
        String xsd = inputStream2String(stream);

        ForeignKeyHelper.overrideSchemaManager(new SchemaMockAgent(xsd, new DataModelID("Product", null)));

        // 1. ForeignKeyInfo = ProductFamily/Name
        MDMConfiguration.getConfiguration().setProperty("xmldb.type", EDBType.QIZX.getName()); //$NON-NLS-1$
        ForeignKeyHelper.ForeignKeyHolder result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model,
                ifFKFilter, value, true);
        WSWhereItem whereItem = result.whereItem;
        assertNotNull(whereItem);
        assertNotNull(whereItem.getWhereAnd());
        assertNotNull(whereItem.getWhereAnd().getWhereItems());

        WSWhereItem whereItem1 = whereItem.getWhereAnd().getWhereItems()[0];
        WSWhereCondition condition1 = whereItem1.getWhereOr().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereAnd()
                .getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Name", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition1.getOperator().getValue());
        assertEquals("Hats", condition1.getRightValueOrPath()); //$NON-NLS-1$

        WSWhereItem whereItem2 = whereItem1.getWhereOr().getWhereItems()[1];
        WSWhereCondition condition2 = whereItem2.getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0]
                .getWhereCondition();
        assertEquals("ProductFamily/Id", condition2.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition2.getOperator().getValue());
        assertEquals("Hats", condition2.getRightValueOrPath()); //$NON-NLS-1$

        // 2. foreignKeyInfo = ProductFamily/Name,ProductFaimly/Description
        foreignKeyInfos.add("ProductFamily/Description"); //$NON-NLS-1$
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);

        List<String> viewableXpaths = result.xpaths;
        assertTrue(viewableXpaths.size() > 0);
        assertEquals("ProductFamily/Name", viewableXpaths.get(0));
        assertEquals("ProductFamily/Description", viewableXpaths.get(1));
        assertEquals("ProductFamily/../../i", viewableXpaths.get(2));

        whereItem = result.whereItem;
        whereItem1 = whereItem.getWhereAnd().getWhereItems()[0];
        condition1 = whereItem1.getWhereOr().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0]
                .getWhereCondition();
        assertEquals("ProductFamily/Name", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition1.getOperator().getValue());
        assertEquals("Hats", condition1.getRightValueOrPath()); //$NON-NLS-1$

        whereItem2 = whereItem1.getWhereOr().getWhereItems()[1];
        condition2 = whereItem2.getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Description", condition2.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition2.getOperator().getValue());
        assertEquals("Hats", condition2.getRightValueOrPath()); //$NON-NLS-1$

        WSWhereItem whereItem3 = whereItem1.getWhereOr().getWhereItems()[2];
        WSWhereCondition condition3 = whereItem3.getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0]
                .getWhereCondition();
        assertEquals("ProductFamily/Id", condition3.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition3.getOperator().getValue());
        assertEquals("Hats", condition3.getRightValueOrPath()); //$NON-NLS-1$

        // 3. foreignKeyInfo is null
        foreignKeyInfos.clear();
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        whereItem1 = whereItem.getWhereAnd().getWhereItems()[0];
        condition1 = whereItem1.getWhereOr().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0]
                .getWhereCondition();
        assertEquals("ProductFamily/../*", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition1.getOperator().getValue());
        assertEquals("Hats", condition1.getRightValueOrPath()); //$NON-NLS-1$

        // 4. foreignKey = ProductFamily, foreignKeyInfo = ProductFamily/Name
        String xpathForeignKey = "ProductFamily"; //$NON-NLS-1$
        String xpathInfoForeignKey = "ProductFamily/Name"; //$NON-NLS-1$
        String realXpathForeignKey = "ProductFamily/Id"; //$NON-NLS-1$
        whereItem = mock_SpecialForeignKeyWhereCondition(xpathForeignKey, xpathInfoForeignKey, realXpathForeignKey, value);

        assertNotNull(whereItem);
        assertNotNull(whereItem.getWhereOr());
        assertNotNull(whereItem.getWhereOr().getWhereItems());
        assertEquals(2, whereItem.getWhereOr().getWhereItems().length);

        whereItem1 = whereItem.getWhereOr().getWhereItems()[0];
        condition1 = whereItem1.getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Name", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition1.getOperator().getValue());
        assertEquals("Hats", condition1.getRightValueOrPath()); //$NON-NLS-1$

        whereItem2 = whereItem.getWhereOr().getWhereItems()[1];
        condition2 = whereItem2.getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Id", condition2.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition2.getOperator().getValue());
        assertEquals("Hats", condition2.getRightValueOrPath()); //$NON-NLS-1$

        // 5. ifFKFilter = true,fkFilter = Product/Family$$<$$.$$#,foreignKeyInfo is null,value is null,
        value = "";
        ifFKFilter = true;
        model.setFkFilter("ProductFamily/Id$$<$$.$$#");
        xml = "<Product><id>1</id><Name>Shirts</Name><Family>[3]</Family></Product>";
        foreignKeyInfos.clear();
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        condition1 = whereItem.getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Id", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._LOWER_THAN, condition1.getOperator().getValue());
        assertEquals("3", condition1.getRightValueOrPath()); //$NON-NLS-1$

        model.setFkFilter("ProductFamily/Id$$<$$.$$And#ProductFamily/Name$$Contains$$../Name$$#");
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        assertEquals(2, whereItem.getWhereAnd().getWhereItems().length);
        condition1 = whereItem.getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Id", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._LOWER_THAN, condition1.getOperator().getValue());
        assertEquals("3", condition1.getRightValueOrPath()); //$NON-NLS-1$
        assertEquals(WSStringPredicate._AND, condition1.getStringPredicate().getValue());
        condition2 = whereItem.getWhereAnd().getWhereItems()[1].getWhereCondition();
        assertEquals("ProductFamily/Name", condition2.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition2.getOperator().getValue());
        assertEquals("Shirts", condition2.getRightValueOrPath()); //$NON-NLS-1$

        // 6. ifFKFilter = true,fkFilter = 'ProductFamily/Name$$Contains$$Product/Name$$#',foreignKeyInfo is null
        value = "";
        ifFKFilter = true;
        model.setFkFilter("ProductFamily/Name$$Contains$$../Name$$#");
        xml = "<Product><id>1</id><Name>Shirts</Name><Family>[3]</Family></Product>";
        foreignKeyInfos.clear();
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        condition1 = whereItem.getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Name", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition1.getOperator().getValue());
        assertEquals("Shirts", condition1.getRightValueOrPath()); //$NON-NLS-1$

        // 7. FK inheritance: ifFKFilter = true,fkFilter = 'ProductType/Type/@xsi:type$$=$$ProductTypeOne$$#'
        /*******************************************************************************************************
         * FK filter note<br>
         * Product/Type is a foreign key<br>
         * -----Foreign Key : ProductType/Id<br>
         * -----Foreign Key Filter : ProductType/Type/@xsi:type$$=$$ProductTypeOne$$#<br>
         * ProductType is an entity(exist three attributes: Id,Name and Type, Type is a comlexType(ComplexProductType))<br>
         * ComplexProductType is an abstract complex type<br>
         * ProductTypeOne inherited ComplexProductType<br>
         * ProductTypeTwo inherited ComplexProductType<br>
         ********************************************************************************************************/
        ifFKFilter = true;
        xml = "<Product><Id>123</Id><Name>zhang</Name><Type>type1</Type></Product>";
        dataCluster = "Product";
        currentXpath = "Product/Type";
        model.setFkFilter("ProductType/Type/@xsi:type$$=$$ProductTypeOne$$#");
        foreignKeyInfos.clear();
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        condition1 = whereItem.getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductType/Type/@xsi:type", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._EQUALS, condition1.getOperator().getValue());
        assertEquals("ProductTypeOne", condition1.getRightValueOrPath()); //$NON-NLS-1$

        xml = "<Product><id>1</id><Name>Shirts</Name><Family>[3]</Family><Type>type1</Type></Product>";
        model.setFkFilter("ProductType/Type/@xsi:type$$=$$ProductTypeTwo$$And#ProductType/Name$$Contains$$/Product/Name$$#");
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        assertEquals(2, whereItem.getWhereAnd().getWhereItems().length);
        condition1 = whereItem.getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductType/Type/@xsi:type", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._EQUALS, condition1.getOperator().getValue());
        assertEquals("ProductTypeTwo", condition1.getRightValueOrPath()); //$NON-NLS-1$
        assertEquals(WSStringPredicate._AND, condition1.getStringPredicate().getValue());
        condition2 = whereItem.getWhereAnd().getWhereItems()[1].getWhereCondition();
        assertEquals("ProductType/Name", condition2.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._CONTAINS, condition2.getOperator().getValue());
        assertEquals("Shirts", condition2.getRightValueOrPath()); //$NON-NLS-1$

        // 8. ifFKFilter = true,fkFilter = 'CategoryOrga/TypeOrgaFK$$=$$OrganisationOperationnelle/OpOrgType$$#'
        ifFKFilter = true;
        value = "";
        dataCluster = "OrganisationOperationnelle";
        currentXpath = "OrganisationOperationnelle/OpOrgCategory";
        xml = "<OrganisationOperationnelle><OpOrgIdentifiantNoeud>1</OpOrgIdentifiantNoeud><OpOrgType>[1]</OpOrgType><OpOrgCategory>aa</OpOrgCategory></OrganisationOperationnelle>";
        model = new SimpleTypeModel("OpOrgCategory", DataTypeConstants.STRING); //$NON-NLS-1$
        foreignKeyInfos.clear();
        foreignKeyInfos.add("CategoryOrga/CategoryOrgaName");
        model.setForeignKeyInfo(foreignKeyInfos);
        model.setForeignkey("CategoryOrga/Id");
        model.setFkFilter("CategoryOrga/TypeOrgaFK$$=$$/OrganisationOperationnelle/OpOrgType$$");
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        condition1 = whereItem.getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("CategoryOrga/TypeOrgaFK", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._EQUALS, condition1.getOperator().getValue());
        assertEquals("1", condition1.getRightValueOrPath()); //$NON-NLS-1$

        // 9. ifFKFilter = true,fkFilter = Product/Family$$=$$"[3]"$$#,foreignKeyInfo is null,value is null,
        value = "";
        ifFKFilter = true;
        model.setFkFilter("ProductFamily/Id$$=$$\"[3]\"$$#");
        xml = "<Product><id>1</id><Name>Shirts</Name><Family>[3]</Family></Product>";
        foreignKeyInfos.clear();
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        condition1 = whereItem.getWhereAnd().getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Id", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._EQUALS, condition1.getOperator().getValue());
        assertEquals("3", condition1.getRightValueOrPath()); //$NON-NLS-1$

        // 10. ifFKFilter = true,fkFilter = $CFFP:Id="[3]" and Name=Product/Name,foreignKeyInfo is null,value is null,
        ifFKFilter = true;
        model.setFkFilter("$CFFP:Id=\"[3]\" and Name=Product/Name");
        xml = "<Product><id>1</id><Name>Shirts</Name><Family>[3]</Family></Product>";
        dataCluster = "Product";
        currentXpath = "Product/Family";
        foreignKeyInfos.clear();
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, "", true);
        assertNull(result.whereItem);
        assertEquals("$CFFP:Id=\"[3]\" and Name=\"Shirts\"", result.fkFilter);

        // 11. foreignKeyinfo is null, DB is SQL db
        MDMConfiguration.getConfiguration().setProperty("xmlserver.class", "com.amalto.core.storage.DispatchWrapper");
        xml = null;
        model.setFkFilter(null);
        ifFKFilter = false;
        foreignKeyInfos.clear();
        value = "1";
        model.setForeignkey("ProductFamily/Id");
        result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model, ifFKFilter, value, true);
        whereItem = result.whereItem;
        whereItem1 = whereItem.getWhereAnd().getWhereItems()[0];
        condition1 = whereItem1.getWhereOr().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereAnd().getWhereItems()[0]
                .getWhereCondition();
        assertTrue(MDMConfiguration.isSqlDataBase());
        assertEquals("ProductFamily/../*", condition1.getLeftPath());
        assertEquals(WSWhereOperator._CONTAINS, condition1.getOperator().getValue());
        assertEquals("1", condition1.getRightValueOrPath());

    }

    public void testGetForeignKeyHolder_given_userInputAs_starWildcard_only_shouldReturn_null_for_WhereItem() throws Exception {

        TypeModel model = new SimpleTypeModel("family", DataTypeConstants.STRING); //$NON-NLS-1$
        model.setForeignkey("ProductFamily/Id"); //$NON-NLS-1$
        List<String> foreignKeyInfos = new ArrayList<String>();
        foreignKeyInfos.add("ProductFamily/Name"); //$NON-NLS-1$
        model.setForeignKeyInfo(foreignKeyInfos);
        model.setRetrieveFKinfos(true);
        model.setXpath("Product/Family"); //$NON-NLS-1$
        boolean ifFKFilter = false;
        String value = "'*'"; //$NON-NLS-1$
        String xml = null;
        String dataCluster = "Product";
        String currentXpath = "Product/Family";

        InputStream stream = getClass().getResourceAsStream("product.xsd");
        String xsd = inputStream2String(stream);

        ForeignKeyHelper.overrideSchemaManager(new SchemaMockAgent(xsd, new DataModelID("Product", null)));

        // ForeignKeyInfo = ProductFamily/Name, user user "*" as only filter value
        ForeignKeyHelper.ForeignKeyHolder result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model,
                ifFKFilter, value, true);
        WSWhereItem whereItem = result.whereItem;
        assertNull(whereItem);
    }
    
    public void testForeignKeyBean_From_ForeignKeyHolder_WhereItem() throws Exception {

        TypeModel model = new SimpleTypeModel("family", DataTypeConstants.STRING); //$NON-NLS-1$
        model.setForeignkey("ProductFamily/Id"); //$NON-NLS-1$
        List<String> foreignKeyInfos = new ArrayList<String>();
        foreignKeyInfos.add("ProductFamily/Name"); //$NON-NLS-1$
        model.setForeignKeyInfo(foreignKeyInfos);
        model.setRetrieveFKinfos(true);
        model.setXpath("Product/Family"); //$NON-NLS-1$
        boolean ifFKFilter = false;
        String value = "Id"; //$NON-NLS-1$
        String xml = null;
        String dataCluster = "Product";
        String currentXpath = "Product/Family";

        InputStream stream = getClass().getResourceAsStream("product.xsd");
        String xsd = inputStream2String(stream);

        ForeignKeyHelper.overrideSchemaManager(new SchemaMockAgent(xsd, new DataModelID("Product", null)));

        // ForeignKeyInfo = ProductFamily/Name, user user "*" as only filter value
        ForeignKeyHelper.ForeignKeyHolder result = ForeignKeyHelper.getForeignKeyHolder(xml, dataCluster, currentXpath, model,
                ifFKFilter, value, false);
        WSWhereItem whereItem = result.whereItem;
        assertNotNull(whereItem);
        assertNotNull(whereItem.getWhereAnd());
        assertNotNull(whereItem.getWhereAnd().getWhereItems());

        WSWhereItem whereItem1 = whereItem.getWhereAnd().getWhereItems()[0];
        WSWhereCondition condition1 = whereItem1.getWhereOr().getWhereItems()[0].getWhereAnd().getWhereItems()[0].getWhereAnd()
                .getWhereItems()[0].getWhereCondition();
        assertEquals("ProductFamily/Id", condition1.getLeftPath()); //$NON-NLS-1$
        assertEquals(WSWhereOperator._EQUALS, condition1.getOperator().getValue());
        assertEquals("Id", condition1.getRightValueOrPath()); //$NON-NLS-1$
        
    }

    private String inputStream2String(InputStream is) {

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuffer buffer = new StringBuffer();
        String line = "";
        try {
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            fail();
        }
        return buffer.toString();

    }

    public void testFormatForeignKeyValue() {

        String value = ForeignKeyHelper.wrapFkValue("123");
        assertEquals("[123]", value);

        value = ForeignKeyHelper.wrapFkValue("[123]");
        assertEquals("[123]", value);

        value = ForeignKeyHelper.unwrapFkValue("[123]");
        assertEquals("123", value);

        value = ForeignKeyHelper.unwrapFkValue("123");
        assertEquals("123", value);

        value = ForeignKeyHelper.unwrapFkValue("[123][456]");
        assertEquals("[123][456]", value);
    }

    /**
     * Mock org.talend.mdm.webapp.base.server.ForeignKeyHelper.getForeignKeyHolder(TypeModel, boolean, String) <li>when
     * the foreignKey = conceptName, using the method to test where condition.
     * 
     * @return
     * @throws Exception
     */
    private WSWhereItem mock_SpecialForeignKeyWhereCondition(String xpathForeignKey, String xpathInfoForeignKey,
            String realXpathForeignKey, String value) throws Exception {
        String initXpathForeignKey = Util.getForeignPathFromPath(xpathForeignKey);
        initXpathForeignKey = initXpathForeignKey.split("/")[0]; //$NON-NLS-1$
        String[] fkInfos = xpathInfoForeignKey.split(","); //$NON-NLS-1$
        String fkWhere = initXpathForeignKey + "/../* CONTAINS " + value; //$NON-NLS-1$
        if (xpathInfoForeignKey.trim().length() > 0) {
            StringBuffer ids = new StringBuffer();
            String realForeignKey = null;
            if (xpathForeignKey.indexOf("/") == -1) { //$NON-NLS-1$
                String[] fks = new String[] { realXpathForeignKey };
                if (fks != null && fks.length > 0) {
                    realForeignKey = fks[0];
                    for (int i = 0; i < fks.length; i++) {
                        ids.append(fks[i] + " CONTAINS " + value); //$NON-NLS-1$
                        if (i != fks.length - 1)
                         {
                            ids.append(" OR "); //$NON-NLS-1$
                        }
                    }
                }
            }
            StringBuffer sb = new StringBuffer();
            for (String fkInfo : fkInfos) {
                sb.append((fkInfo.startsWith(".") ? Util.convertAbsolutePath((realForeignKey != null && realForeignKey.trim() //$NON-NLS-1$
                        .length() > 0) ? realForeignKey : xpathForeignKey, fkInfo) : fkInfo) + " CONTAINS " + value); //$NON-NLS-1$
                sb.append(" OR "); //$NON-NLS-1$
            }
            if (realForeignKey != null) {
                sb.append(ids.toString());
            }
            else {
                sb.append(xpathForeignKey + " CONTAINS " + value); //$NON-NLS-1$
            }
            fkWhere = sb.toString();
        }

        return Util.buildWhereItems(fkWhere);
    }

    public void testConvertFKInfo2DisplayInfo() {
        ForeignKeyBean bean1 = new ForeignKeyBean();
        bean1.set("Name", "Apple Store");
        bean1.set("Id", "1");
        bean1.set("Code", "10001");
        bean1.set("i", "1");
        bean1.set("Description", "[EN:Talend MDM][FR:Talend DQ]");
        bean1.getForeignKeyInfo().put("Store/Id", "1");
        bean1.getForeignKeyInfo().put("Store/Code", "10001");
        bean1.getForeignKeyInfo().put("Store/Name", "Apple Store");
        bean1.getForeignKeyInfo().put("Stroe/Description",
                MultilanguageMessageParser.getValueByLanguage((String) bean1.get("Description"), "en"));

        List<String> fkInfoList = new ArrayList<String>();
        fkInfoList.add("Store/Name");
        fkInfoList.add("Store/Code");
        fkInfoList.add("Store/Id");
        fkInfoList.add("Stroe/Description");

        ForeignKeyHelper.convertFKInfo2DisplayInfo(bean1, fkInfoList);

        assertNotNull(bean1.getDisplayInfo());
        assertEquals("Apple Store-10001-1-Talend MDM", bean1.getDisplayInfo());

        ForeignKeyBean bean2 = new ForeignKeyBean();
        bean2.set("Name", "Google Market");
        bean2.set("Id", "2");
        bean2.set("Code", "10002");
        bean2.set("i", "2");
        bean2.getForeignKeyInfo().put("Store/Id", "2");
        bean2.getForeignKeyInfo().put("Store/Code", "10002");
        bean2.getForeignKeyInfo().put("Store/Name", "Google Market");

        fkInfoList.clear();

        ForeignKeyHelper.convertFKInfo2DisplayInfo(bean2, fkInfoList);
        assertNull(bean2.getDisplayInfo());
    }

    public void testIsPolymorphismTypeFK() throws Exception {
        InputStream stream = getClass().getResourceAsStream("metadata.xsd");
        String xsd = inputStream2String(stream);

        SchemaMockAgent schemaMockAgent = new SchemaMockAgent(xsd, new DataModelID("Territory", null));
        assertFalse(schemaMockAgent.isPolymorphismTypeFK("Country"));

        stream = getClass().getResourceAsStream("person.xsd");
        xsd = inputStream2String(stream);

        schemaMockAgent = new SchemaMockAgent(xsd, new DataModelID("ProductEntity", null));
        assertTrue(schemaMockAgent.isPolymorphismTypeFK("Party"));
        assertTrue(schemaMockAgent.isPolymorphismTypeFK("Individual"));
        assertTrue(schemaMockAgent.isPolymorphismTypeFK("Company"));
        assertFalse(schemaMockAgent.isPolymorphismTypeFK("Family"));

    }

    public void testInitFKBean() throws Exception {
        String[] results = new String[] {
                "<result>\n\t<C_Name>cname1</C_Name>\n\t<C_Title>ctitle1</C_Title>\n\t<C_Sub_Name>csubname1</C_Sub_Name>\n\t<C_Sub_Title>csubtitle1</C_Sub_Title>\n\t<C_Sub_Sub_Name>csubsubname1</C_Sub_Sub_Name>\n\t<C_Sub_Sub_Title>csubsubtitle1</C_Sub_Sub_Title>\n\t<i>c1</i>\n</result>",
                "<result>\n\t<C_Name>cname2</C_Name>\n\t<C_Title>ctitle2</C_Title>\n\t<C_Sub_Name>csubname2</C_Sub_Name>\n\t<C_Sub_Title>csubtitle2</C_Sub_Title>\n\t<C_Sub_Sub_Name>csubsubname2</C_Sub_Sub_Name>\n\t<C_Sub_Sub_Title>csubsubtitle2</C_Sub_Sub_Title>\n\t<i>c2</i>\n</result>" };
        String[] fKInfoExpected = new String[] { "cname1-ctitle1-csubname1-csubtitle1-csubsubname1-csubsubtitle1",
                "cname2-ctitle2-csubname2-csubtitle2-csubsubname2-csubsubtitle2" };

        String dataClusterPK = "TestFK";
        EntityModel entityModel = new EntityModel();
        entityModel.setConceptName("C");
        entityModel.setKeys(new String[] { "C/C_Id" });
        String fk = "C";
        List<String> foreignKeyInfos = Arrays.asList("C/C_Name", "C/C_Title", "C/C_SubInfo/C_Sub_Name",
                "C/C_SubInfo/C_Sub_Title", "C/C_SubInfo/C_SubSubInfo/C_Sub_Sub_Name", "C/C_SubInfo/C_SubSubInfo/C_Sub_Sub_Title");
        Map<String, String> xpathTypeMap = new HashMap<String, String>();
        String language = "zh";
        ForeignKeyBean[] fkBeans = new ForeignKeyBean[] { new ForeignKeyBean(), new ForeignKeyBean() };
        fkBeans[0].setId("c1");
        fkBeans[1].setId("c2");

        Element[] resultAsDOM = new Element[2];
        for (int i = 0; i < fkBeans.length; i++) {
            resultAsDOM[i] = Util.parse(results[i]).getDocumentElement();

            ForeignKeyHelper.initFKBean(dataClusterPK, entityModel, resultAsDOM[i], fkBeans[i], fk, foreignKeyInfos,
                    xpathTypeMap, language);
            ForeignKeyHelper.convertFKInfo2DisplayInfo(fkBeans[i], foreignKeyInfos);
        }

        for (int i = 0; i < fkBeans.length; i++) {
            assertEquals(fkBeans[i].getId() + "->Expected displayInfo is not equal to acutal displayInfo", fKInfoExpected[i],
                    fkBeans[i].getDisplayInfo());
        }
    }

    public void testInitFKBean_for_missing_values_In_FKInfos() throws Exception {
        String[] results = new String[] { "<result>\n\t<Name>bname0</Name>\n\t<CF>cname1</CF>\n\t<i>b0</i>\n</result>",
                "<result>\n\t<CF>cname1</CF>\n\t<i>b1</i>\n</result>", "<result>\n\t<Name>bname2</Name>\n\t<i>b2</i>\n</result>",
                "<result>\n\t<i>b2</i>\n</result>" };
        String[] fKInfoExpected = new String[] { "bname0-cname1", "cname1", "bname2", "" };

        String dataClusterPK = "TestFK";
        EntityModel entityModel = new EntityModel();
        entityModel.setConceptName("B");
        entityModel.setKeys(new String[] { "B/subelement" });
        String fk = "B";
        List<String> foreignKeyInfos = Arrays.asList("B/Name", "B/CF");
        Map<String, String> xpathTypeMap = new HashMap<String, String>();
        String language = "zh";
        ForeignKeyBean[] fkBeans = new ForeignKeyBean[] { new ForeignKeyBean(), new ForeignKeyBean(), new ForeignKeyBean(),
                new ForeignKeyBean() };
        fkBeans[0].setId("b0");
        fkBeans[1].setId("b1");
        fkBeans[2].setId("b2");
        fkBeans[3].setId("b3");

        Element[] resultAsDOM = new Element[4];
        for (int i = 0; i < fkBeans.length; i++) {
            resultAsDOM[i] = Util.parse(results[i]).getDocumentElement();

            ForeignKeyHelper.initFKBean(dataClusterPK, entityModel, resultAsDOM[i], fkBeans[i], fk, foreignKeyInfos,
                    xpathTypeMap, language);
            ForeignKeyHelper.convertFKInfo2DisplayInfo(fkBeans[i], foreignKeyInfos);
        }

        for (int i = 0; i < fkBeans.length; i++) {
            assertEquals(fkBeans[i].getId() + "-> Expected displayInfo is not equal to actual displayInfo", fKInfoExpected[i],
                    fkBeans[i].getDisplayInfo());
        }
    }
}
