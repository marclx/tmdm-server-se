package com.amalto.core.storage.hibernate;

import org.apache.commons.lang.StringUtils;
import org.talend.mdm.commmon.metadata.*;

import javax.xml.XMLConstants;
import java.util.Collections;

/**
 *
 */
class UpdateReportMappingCreator extends DefaultMetadataVisitor<TypeMapping> {

    private MappingRepository mappings;

    private final ComplexTypeMetadata DATABASE_UPDATE_REPORT_TYPE;

    private final ComplexTypeMetadata USER_UPDATE_REPORT_TYPE;

    private final MetadataRepository repository;

    public UpdateReportMappingCreator(TypeMetadata updateReportType, MetadataRepository repository, MappingRepository mappings) {
        this.repository = repository;
        if(updateReportType == null) {
            throw new IllegalStateException("Update report type cannot be null.");
        }
        USER_UPDATE_REPORT_TYPE = (ComplexTypeMetadata) updateReportType;

        this.mappings = mappings;
        ComplexTypeMetadata databaseUpdateReportType = new ComplexTypeMetadataImpl(StringUtils.EMPTY, "X_UPDATE_REPORT", true); //$NON-NLS-1$
        TypeMetadata stringType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, Types.STRING);
        TypeMetadata longStringType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, Types.STRING);
        TypeMetadata longType = new SimpleTypeMetadata(XMLConstants.W3C_XML_SCHEMA_NS_URI, Types.LONG);
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_user_name", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, true, false, true, "x_source", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, true, false, true, "x_time_in_millis", longType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_operation_type", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_revision_id", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_data_cluster", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_data_model", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_concept", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        databaseUpdateReportType.addField(new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_key", stringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY)); //$NON-NLS-1$
        SimpleTypeFieldMetadata items_xml = new SimpleTypeFieldMetadata(databaseUpdateReportType, false, false, false, "x_items_xml", longStringType, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), StringUtils.EMPTY); //$NON-NLS-1$
        items_xml.getType().setData(TypeMapping.SQL_TYPE, "text"); //$NON-NLS-1$
        databaseUpdateReportType.addField(items_xml);
        DATABASE_UPDATE_REPORT_TYPE = (ComplexTypeMetadata) databaseUpdateReportType.freeze();
    }

    @Override
    public TypeMapping visit(ComplexTypeMetadata complexType) {
        if(!"Update".equals(complexType.getName())) {
            throw new IllegalArgumentException("Expected a type 'Update' but got '" + complexType.getName() + "'.");
        }
        return new UpdateReportTypeMapping(USER_UPDATE_REPORT_TYPE, DATABASE_UPDATE_REPORT_TYPE, mappings, repository);
    }
}
