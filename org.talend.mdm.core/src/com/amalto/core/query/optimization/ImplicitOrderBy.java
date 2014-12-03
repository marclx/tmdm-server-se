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
package com.amalto.core.query.optimization;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;

import com.amalto.core.query.user.Alias;
import com.amalto.core.query.user.Count;
import com.amalto.core.query.user.Distinct;
import com.amalto.core.query.user.Field;
import com.amalto.core.query.user.Max;
import com.amalto.core.query.user.Min;
import com.amalto.core.query.user.OrderBy;
import com.amalto.core.query.user.Select;
import com.amalto.core.query.user.TypedExpression;
import com.amalto.core.query.user.UserQueryDumpConsole;
import com.amalto.core.storage.datasource.RDBMSDataSource;


public class ImplicitOrderBy implements Optimizer {
    
    private static final Logger LOGGER = Logger.getLogger(ImplicitOrderBy.class);
    
    private final RDBMSDataSource dataSource;

    public ImplicitOrderBy(RDBMSDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void optimize(Select select) {
        switch (dataSource.getDialectName()) {
        case H2:
            return;
        case ORACLE_10G:
            return;
        case MYSQL:
            return;
        case SQL_SERVER:
            return;
        case DB2:
            return;
        case POSTGRES:
            if (select.getPaging().getLimit() < Integer.MAX_VALUE && !select.getTypes().isEmpty()) { // has paging defined
                if (select.getOrderBy() == null || select.getOrderBy().size() == 0) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Adding implicit order by id to keep consistent order trough pages."); //$NON-NLS-1$
                    }
                    ComplexTypeMetadata typeMetadata = select.getTypes().get( 0 );
                    Collection<FieldMetadata> keyFields = typeMetadata.getKeyFields();
                    List<TypedExpression> list = select.getSelectedFields();                    
                    Boolean addOrderEnable = true;
                    
                    if(list != null && list.size() > 0){                        
                        for(TypedExpression te : list){
                            if(te instanceof Alias){
                                String aliasName = ((Alias)te).getAliasName();
                                if(aliasName != null && (aliasName.equalsIgnoreCase("count")  //$NON-NLS-1$
                                        || aliasName.equalsIgnoreCase("distinct")   //$NON-NLS-1$
                                        || aliasName.equalsIgnoreCase("max")   //$NON-NLS-1$
                                        || aliasName.equalsIgnoreCase("min"))){ //$NON-NLS-1$
                                    addOrderEnable = false;
                                    break;
                                }
                            } else if(te instanceof Count || te instanceof Distinct || te instanceof Max  || te instanceof Min){
                                addOrderEnable = false;
                                break;
                            }
                        }
                    }

                    if(addOrderEnable){
                        select.addOrderBy(new OrderBy(new Field(keyFields.iterator().next()), OrderBy.Direction.ASC));
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Order by not supported by this query: "); //$NON-NLS-1$
                            select.accept(new UserQueryDumpConsole(LOGGER));
                        }
                    }
                }
            }
            return;
        default:
            throw new IllegalArgumentException("No support for dialect '" + dataSource.getDialectName() + "'.");  //$NON-NLS-1$//$NON-NLS-2$
        }
    }
}