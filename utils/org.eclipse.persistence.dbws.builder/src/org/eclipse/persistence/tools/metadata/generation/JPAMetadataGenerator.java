/*******************************************************************************
 * Copyright (c) 1998, 2013 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     David McCann - 2.6.0 - July 09, 2013 - Initial Implementation
 ******************************************************************************/
package org.eclipse.persistence.tools.metadata.generation;

import static org.eclipse.persistence.internal.jpa.metadata.MetadataConstants.EL_ACCESS_VIRTUAL;
import static org.eclipse.persistence.tools.metadata.generation.Util.ARRAYLIST_STR;
import static org.eclipse.persistence.tools.metadata.generation.Util.CURSOR_STR;
import static org.eclipse.persistence.tools.metadata.generation.Util.DOT;
import static org.eclipse.persistence.tools.metadata.generation.Util.ITEMS_COL_STR;
import static org.eclipse.persistence.tools.metadata.generation.Util.ITEMS_FLD_STR;
import static org.eclipse.persistence.tools.metadata.generation.Util.OUT_CURSOR_STR;
import static org.eclipse.persistence.tools.metadata.generation.Util.PERCENT;
import static org.eclipse.persistence.tools.metadata.generation.Util.RESULT_STR;
import static org.eclipse.persistence.tools.metadata.generation.Util.ROWTYPE_STR;
import static org.eclipse.persistence.tools.metadata.generation.Util.UNDERSCORE;
import static org.eclipse.persistence.tools.metadata.generation.Util.getClassNameFromJDBCTypeName;
import static org.eclipse.persistence.tools.metadata.generation.Util.getEntityName;
import static org.eclipse.persistence.tools.metadata.generation.Util.getGeneratedJavaClassName;
import static org.eclipse.persistence.tools.metadata.generation.Util.getJDBCTypeFromTypeName;
import static org.eclipse.persistence.tools.metadata.generation.Util.getJDBCTypeName;
import static org.eclipse.persistence.tools.metadata.generation.Util.getOraclePLSQLTypeForName;
import static org.eclipse.persistence.tools.metadata.generation.Util.getQualifiedCompatibleTypeName;
import static org.eclipse.persistence.tools.metadata.generation.Util.getQualifiedTypeName;
import static org.eclipse.persistence.tools.metadata.generation.Util.isArgPLSQLScalar;
import static org.eclipse.persistence.tools.metadata.generation.Util.processTypeName;

import java.security.AccessController;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.internal.databaseaccess.DatabasePlatform;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EmbeddableAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EntityAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.XMLAttributes;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.BasicAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.EmbeddedAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.IdAccessor;
import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.NamedNativeQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.NamedPLSQLStoredFunctionQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.NamedPLSQLStoredProcedureQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.NamedStoredFunctionQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.NamedStoredProcedureQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.OracleArrayTypeMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.OracleObjectTypeMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.PLSQLParameterMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.PLSQLRecordMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.PLSQLTableMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.StoredProcedureParameterMetadata;
import org.eclipse.persistence.internal.jpa.metadata.structures.ArrayAccessor;
import org.eclipse.persistence.internal.jpa.metadata.structures.StructMetadata;
import org.eclipse.persistence.internal.jpa.metadata.structures.StructureAccessor;
import org.eclipse.persistence.internal.jpa.metadata.tables.TableMetadata;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.tools.oracleddl.metadata.ArgumentType;
import org.eclipse.persistence.tools.oracleddl.metadata.ArgumentTypeDirection;
import org.eclipse.persistence.tools.oracleddl.metadata.CompositeDatabaseType;
import org.eclipse.persistence.tools.oracleddl.metadata.CompositeDatabaseTypeWithEnclosedType;
import org.eclipse.persistence.tools.oracleddl.metadata.DatabaseType;
import org.eclipse.persistence.tools.oracleddl.metadata.FieldType;
import org.eclipse.persistence.tools.oracleddl.metadata.FunctionType;
import org.eclipse.persistence.tools.oracleddl.metadata.ObjectTableType;
import org.eclipse.persistence.tools.oracleddl.metadata.ObjectType;
import org.eclipse.persistence.tools.oracleddl.metadata.PLSQLCollectionType;
import org.eclipse.persistence.tools.oracleddl.metadata.PLSQLPackageType;
import org.eclipse.persistence.tools.oracleddl.metadata.PLSQLRecordType;
import org.eclipse.persistence.tools.oracleddl.metadata.PLSQLType;
import org.eclipse.persistence.tools.oracleddl.metadata.ProcedureType;
import org.eclipse.persistence.tools.oracleddl.metadata.ROWTYPEType;
import org.eclipse.persistence.tools.oracleddl.metadata.TYPEType;
import org.eclipse.persistence.tools.oracleddl.metadata.TableType;
import org.eclipse.persistence.tools.oracleddl.metadata.VArrayType;

/**
 * This class is responsible for generating an XMLEntityMappings instance based
 * on a given list of meta-model database types.
 * 
 * @see org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings
 * @see org.eclipse.persistence.tools.oracleddl.metadata.CompositeDatabaseType
 */
public class JPAMetadataGenerator {
    protected DatabasePlatform dbPlatform;
    protected String defaultPackage;
    
    protected XMLEntityMappings xmlEntityMappings;
    
    // PL/SQL records and collections, and Oracle advanced JDBC types
    protected List<PLSQLRecordMetadata> plsqlRecords = null;
    protected List<PLSQLTableMetadata>  plsqlTables  = null;
    protected List<OracleObjectTypeMetadata> objectTypes = null;
    protected List<OracleArrayTypeMetadata> arrayTypes = null;

    // queries
    protected List<NamedPLSQLStoredProcedureQueryMetadata> plsqlStoredProcs = null;
    protected List<NamedPLSQLStoredFunctionQueryMetadata> plsqlStoredFuncs = null;
    protected List<NamedStoredProcedureQueryMetadata> storedProcs = null;
    protected List<NamedStoredFunctionQueryMetadata> storedFuncs = null;
    protected List<NamedNativeQueryMetadata> namedNativeQueries = null;
    
    // keep track of processed composite types to avoid duplicates and wasted effort
    protected List<String> processedTypes = null;

    // keep track of processed embeddables to avoid duplicates and wasted effort
    protected List<String> generatedEmbeddables = null;
    
    protected static final String DEFAULT_PLATFORM = "org.eclipse.persistence.platform.database.oracle.Oracle11Platform";
    
    /**
     * Default constructor.  Sets the default package name to null, and dbPlatform to
     * org.eclipse.persistence.platform.database.oracle.Oracle11Platform.
     * 
     * The default package name will be prepended to generated class names for database
     * artifacts that are not in a PL/SQL package.
     * 
     * The database platform is used to get class names for database types, i.e. 
     * java.math.BigDecimal for DECIMAL.
     * 
     * @see org.eclipse.persistence.platform.database.oracle.Oracle11Platform
     * @see org.eclipse.persistence.internal.databaseaccess.DatabasePlatform
     */
    public JPAMetadataGenerator() {
        this(null, DEFAULT_PLATFORM);
    }
    
    /**
     * This constructor allows setting the default package name and database platform.
     * 
     * @param defaultPackage package name to be prepended to generated class names for artifacts
     * not in a PL/SQL package such as an Entity (to avoid having classes in the default package)
     * @param platformClassName class name of the DatabasePlatform to be used to get class names 
     * for database types, i.e. java.math.BigDecimal for DECIMAL.
     * @see org.eclipse.persistence.internal.databaseaccess.DatabasePlatform
     */
    public JPAMetadataGenerator(String defaultPackage, String platformClassName) {
        this(defaultPackage, loadDatabasePlatform(platformClassName));
    }    

    /**
     * This constructor allows setting the default package name and database platform.
     * 
     * @param defaultPackage package name to be prepended to generated class names for artifacts
     * not in a PL/SQL package such as an Entity (to avoid having classes in the default package)
     * @param dbPlatform DatabasePlatform to be used to get class names for database types, i.e. 
     * java.math.BigDecimal for DECIMAL.
     * @see org.eclipse.persistence.internal.databaseaccess.DatabasePlatform
     */
    public JPAMetadataGenerator(String defaultPackage, DatabasePlatform dbPlatform) {
        this.defaultPackage = defaultPackage;
        this.dbPlatform = dbPlatform;
        
        xmlEntityMappings = new XMLEntityMappings();
        xmlEntityMappings.setEmbeddables(new ArrayList<EmbeddableAccessor>());
        xmlEntityMappings.setEntities(new ArrayList<EntityAccessor>());

        xmlEntityMappings.setPLSQLRecords(new ArrayList<PLSQLRecordMetadata>());
        xmlEntityMappings.setPLSQLTables(new ArrayList<PLSQLTableMetadata>());
        xmlEntityMappings.setOracleObjectTypes(new ArrayList<OracleObjectTypeMetadata>());
        xmlEntityMappings.setOracleArrayTypes(new ArrayList<OracleArrayTypeMetadata>());
    }    
    
    /**
     * Generate an XMLEntityMappings instance based on a given list of meta-model database types.
     * 
     * @param databaseTypes the list of meta-model database types to be used to generate an XMLEntityMappings
     * @see org.eclipse.persistence.tools.oracleddl.metadata.CompositeDatabaseType
     */
    public XMLEntityMappings generateXmlEntityMappings(List<CompositeDatabaseType> databaseTypes) {
        // process DatabaseTypes
        for (CompositeDatabaseType dbType : databaseTypes) {
            if (dbType.isTableType()) {  // process TableType
                EntityAccessor entity = processTableType((TableType) dbType);
                xmlEntityMappings.getEntities().add(entity);
            } else if (dbType.isProcedureType()) {  // process functions and procedures
                ProcedureType pType = (ProcedureType) dbType;
                // PL/SQL stored procedures and functions will have a PLSQLPackageType as its parent
                PLSQLPackageType pkgType = pType.getParentType();
                if (pkgType != null) {
                    // handle PL/SQL
                    if (pType.isFunctionType()) {
                        getPlsqlStoredFuncs().add(processPLSQLFunctionType((FunctionType) pType, pkgType));
                    } else {
                        getPlsqlStoredProcs().add(processPLSQLProcedureType(pType, pkgType));
                    }
                } else {
                    // handle top-level (non-PL/SQL) functions and procedures
                    if (pType.isFunctionType()) {
                        getStoredFuncs().add(processFunctionType((FunctionType) pType));
                    } else {
                        getStoredProcs().add(processProcedureType(pType));
                    }
                }
            }
        }
        // now add any generated metadata to the XMLEntityMappings instance
        applyGeneratedMetadata();

        return xmlEntityMappings;
    }
    
    /**
     * Generate an EntityAccessor based on the given TableType.
     */
    protected EntityAccessor processTableType(TableType tType) {
        EntityAccessor entity = new EntityAccessor();
        entity.setAccess(EL_ACCESS_VIRTUAL);
        entity.setClassName(getEntityName(tType.getTableName(), defaultPackage));
        
        entity.setAttributes(new XMLAttributes());
        entity.getAttributes().setIds(new ArrayList<IdAccessor>());
        entity.getAttributes().setBasics(new ArrayList<BasicAccessor>());
        
        // set the table name on the entity
        TableMetadata table = new TableMetadata();
        table.setName(tType.getTableName());
        entity.setTable(table);
        
        // process the table columns
        for (FieldType fType : tType.getColumns()) {
            BasicAccessor attribute;
            // handle primary key
            if (fType.pk()) {
                attribute = new IdAccessor();
                entity.getAttributes().getIds().add((IdAccessor) attribute);
            } else {
                attribute = new BasicAccessor();
                entity.getAttributes().getBasics().add((BasicAccessor) attribute);
            }
            attribute.setName(fType.getFieldName().toLowerCase());
            attribute.setAttributeType(getClassNameFromJDBCTypeName(fType.getTypeName(), dbPlatform));
            // set the column name
            ColumnMetadata column = new ColumnMetadata();
            column.setName(fType.getFieldName());
            attribute.setColumn(column);
        }
        return entity;
    }
    
    /**
     * Generate a stored function query based on the given FunctionType.
     */
    protected NamedStoredFunctionQueryMetadata processFunctionType(FunctionType fType) {
        NamedStoredFunctionQueryMetadata storedFunc = new NamedStoredFunctionQueryMetadata();
        storedFunc.setName(fType.getProcedureName());
        storedFunc.setProcedureName(fType.getProcedureName());
        // set the return parameter
        storedFunc.setReturnParameter(processArgument(fType.getReturnArgument()));
        // process the function's arguments
        if (fType.getArguments().size() > 0) {
            List<StoredProcedureParameterMetadata> params = new ArrayList<StoredProcedureParameterMetadata>();
            for (ArgumentType arg : fType.getArguments()) {
                params.add(processArgument(arg));
            }
            storedFunc.setParameters(params);
        }                
        return storedFunc;
    }
    
    /**
     * Generate a stored procedure query based on the given ProcedureType.
     */
    protected NamedStoredProcedureQueryMetadata processProcedureType(ProcedureType pType) {
        NamedStoredProcedureQueryMetadata storedProc = new NamedStoredProcedureQueryMetadata();
        storedProc.setName(pType.getProcedureName());
        storedProc.setProcedureName(pType.getProcedureName());
        storedProc.setReturnsResultSet(false);
        // process the procedure's arguments
        if (pType.getArguments().size() > 0) {
            List<StoredProcedureParameterMetadata> params = new ArrayList<StoredProcedureParameterMetadata>();
            for (ArgumentType arg : pType.getArguments()) {
                params.add(processArgument(arg));
            }
            storedProc.setParameters(params);
        }
        return storedProc;
    }
    
    /**
     * Generate a PL/SQL stored function query based on the given FunctionType.
     */
    protected NamedPLSQLStoredFunctionQueryMetadata processPLSQLFunctionType(FunctionType fType, PLSQLPackageType pkgType) {
        NamedPLSQLStoredFunctionQueryMetadata storedFunc = new NamedPLSQLStoredFunctionQueryMetadata();
        storedFunc.setName(fType.getProcedureName());
        storedFunc.setProcedureName(pkgType.getPackageName() + DOT + fType.getProcedureName());
        List<PLSQLParameterMetadata> params = new ArrayList<PLSQLParameterMetadata>();
        // set the return parameter
        storedFunc.setReturnParameter(processPLSQLArgument(fType.getReturnArgument()));
        // process the function's arguments
        if (fType.getArguments().size() > 0) {
            for (ArgumentType arg : fType.getArguments()) {
                params.add(processPLSQLArgument(arg));
            }
        }                
        storedFunc.setParameters(params);
        return storedFunc;
    }
    
    /**
     * Generate a PL/SQL stored procedure query based on the given ProcedureType.
     */
    protected NamedPLSQLStoredProcedureQueryMetadata processPLSQLProcedureType(ProcedureType pType, PLSQLPackageType pkgType) {
        NamedPLSQLStoredProcedureQueryMetadata storedProc = new NamedPLSQLStoredProcedureQueryMetadata();
        storedProc.setName(pType.getProcedureName());
        storedProc.setProcedureName(pkgType.getPackageName() + DOT + pType.getProcedureName());
        // process the procedure's arguments
        if (pType.getArguments().size() > 0) {
            List<PLSQLParameterMetadata> params = new ArrayList<PLSQLParameterMetadata>();
            for (ArgumentType arg : pType.getArguments()) {
                params.add(processPLSQLArgument(arg));
            }
            storedProc.setParameters(params);
        }
        return storedProc;
    }

    /**
     * Generate a stored procedure parameter based on the given ArgumentType.
     * For PL/SQL arguments the processPLSQLArgument method should be used.
     */
    protected StoredProcedureParameterMetadata processArgument(ArgumentType arg) {
        StoredProcedureParameterMetadata param = new StoredProcedureParameterMetadata();
        param.setName(arg.getArgumentName());
        // don't set mode for stored function return argument
        if (arg.getDirection() != ArgumentTypeDirection.RETURN) {
            param.setMode(arg.getDirection().name());
        }
        if (!arg.isComposite()) {
            param.setTypeName(getClassNameFromJDBCTypeName(arg.getTypeName(), dbPlatform));
            param.setJdbcType(getJDBCTypeFromTypeName(arg.getTypeName()));
            param.setJdbcTypeName(getJDBCTypeName(arg.getTypeName()));
        } else {  // handle composites, i.e. Object, Varray, PL/SQL, etc.
            param.setTypeName(getGeneratedJavaClassName(arg.getTypeName(), defaultPackage));
            param.setJdbcTypeName(arg.getTypeName());
            if (arg.isObjectType()) {  // ObjectType
                param.setJdbcType(Types.STRUCT);
            } else if (arg.isObjectTableType() || arg.isVArrayType()) {  // ObjectTable and Varray
                param.setJdbcType(Types.ARRAY);
            }
            processCompositeType(arg.getEnclosedType());
        }
        return param;
    }
    
    /**
     * Generate a PL/SQL parameter based on the given ArgumentType. For
     * non-PL/SQL arguments the processArgument method should be used.
     */
    protected PLSQLParameterMetadata processPLSQLArgument(ArgumentType arg) {
        // for %ROWTYPE, we need to create a PL/SQL record that mirrors the Table
        if (arg.getEnclosedType().isROWTYPEType()) {
            ROWTYPEType rType = (ROWTYPEType) arg.getEnclosedType();
            TableType tableType = (TableType) rType.getEnclosedType();
            PLSQLRecordType plsqlRec = new PLSQLRecordType(rType.getTypeName());
            plsqlRec.setParentType(new PLSQLPackageType());
            for (FieldType col : tableType.getColumns()) {
                FieldType ft = new FieldType(col.getFieldName());
                ft.setEnclosedType(col.getEnclosedType());
                plsqlRec.addField(ft);
            }
            arg.setEnclosedType(plsqlRec);
        }
        
        PLSQLParameterMetadata param = new PLSQLParameterMetadata();
        // handle cursor
        if (arg.isPLSQLCursorType()) {
            param.setDirection(OUT_CURSOR_STR);
        }
        // handle stored function return type
        if (arg.getDirection() == ArgumentTypeDirection.RETURN) {
            param.setName(arg.isPLSQLCursorType() ? CURSOR_STR : RESULT_STR);
        } else {
            // direction is already set for cursor type
            if (!arg.isPLSQLCursorType()) {
                param.setDirection(arg.getDirection().name());
            }
            param.setName(arg.getArgumentName());
        }
        
        String dbType = arg.getTypeName();
        // handle composites
        if (arg.isComposite()) {
            DatabaseType enclosedType = arg.getEnclosedType();
            // need to prepend the package name for most PL/SQL and Cursor types
            if (enclosedType.isPLSQLType() || enclosedType.isPLSQLCursorType()) {
                dbType = getQualifiedTypeName(enclosedType);
            }
            // process the composite enclosed type
            processCompositeType(enclosedType, dbType);
        }
        param.setDatabaseType(processTypeName(dbType));
        return param;
    }
    
    /**
     * Generate object type metadata based on the given ObjectType.
     */
    protected OracleObjectTypeMetadata processObjectType(ObjectType oType) {
        OracleObjectTypeMetadata objectType = new OracleObjectTypeMetadata();
        objectType.setName(oType.getTypeName());
        objectType.setJavaType(getGeneratedJavaClassName(oType.getTypeName(), defaultPackage));
        // process the object type's fields
        List<PLSQLParameterMetadata> fields = new ArrayList<PLSQLParameterMetadata>();
        for (FieldType ft : oType.getFields()) {
            PLSQLParameterMetadata fieldMetadata = new PLSQLParameterMetadata();
            fieldMetadata.setName(ft.getFieldName());
            fieldMetadata.setDatabaseType(processTypeName(ft.getTypeName()));
            fields.add(fieldMetadata);
            if (ft.isComposite()) {
                processCompositeType(ft.getEnclosedType());
            }
        }
        objectType.setFields(fields);
        
        // avoid double-processing
        getProcessedTypes().add(objectType.getName());
        
        // generate an EmbeddableAccessor for this type
        generateEmbeddable(objectType, oType);

        return objectType;
    }
    
    /**
     * Generate array type metadata based on the given VArray or ObjectTable type.
     */
    protected OracleArrayTypeMetadata processArrayType(DatabaseType dbType) {
        OracleArrayTypeMetadata arrayType = new OracleArrayTypeMetadata();
        arrayType.setName(dbType.getTypeName());
        arrayType.setJavaType(getGeneratedJavaClassName(dbType.getTypeName(), defaultPackage));
        if (dbType.isVArrayType()) {
            arrayType.setNestedType(processTypeName(((VArrayType) dbType).getEnclosedType().getTypeName()));
        } else {
            // assumes ObjectTable
            arrayType.setNestedType(((ObjectTableType) dbType).getEnclosedType().getTypeName());
        }
        // avoid double-processing
        getProcessedTypes().add(arrayType.getName());     
        
        // generate an EmbeddableAccessor for this type
        generateEmbeddable(arrayType, (CompositeDatabaseTypeWithEnclosedType) dbType);

        return arrayType;
    }
    
    /**
     * Process the given PLSQLCollectionType and return a PLSQLTableMetadata instance.
     * 
     */
    protected PLSQLTableMetadata processPLSQLCollectionType(PLSQLCollectionType plsqlCollectionType) {
        String typeName = getQualifiedTypeName(plsqlCollectionType);
        String compatiableName = getQualifiedCompatibleTypeName(plsqlCollectionType);
        String targetClassName = compatiableName;
        
        PLSQLTableMetadata plsqlTable = new PLSQLTableMetadata();
        plsqlTable.setName(typeName);
        plsqlTable.setCompatibleType(compatiableName);
        plsqlTable.setJavaType(getGeneratedJavaClassName(typeName));
        
        // handle Nested Table (i.e. non-Varray)
        plsqlTable.setNestedTable(!plsqlCollectionType.isIndexed());
        String dbType = plsqlCollectionType.getEnclosedType().getTypeName();
        if (!(getJDBCTypeFromTypeName(dbType) == Types.OTHER)) {          
            // need special handling for nested PL/SQL scalar types
            if (isArgPLSQLScalar(dbType)) {
                plsqlTable.setNestedType(getOraclePLSQLTypeForName(dbType));
            } else {
                plsqlTable.setNestedType(processTypeName(dbType));
            }
        } else {
            if (plsqlCollectionType.isComposite()) {
                DatabaseType enclosedType = plsqlCollectionType.getEnclosedType();
                // may need to prepend package name
                if (enclosedType.isPLSQLType()) {
                    dbType = ((PLSQLType) enclosedType).getParentType().getPackageName() + DOT + dbType;
                    targetClassName = getGeneratedJavaClassName(dbType);
                } else {
                    // advanced JDBC
                    targetClassName = getGeneratedJavaClassName(dbType, defaultPackage);
                }
                processCompositeType(enclosedType, dbType);
            }
            plsqlTable.setNestedType(dbType);
        }
        // avoid double-processing
        getProcessedTypes().add(plsqlTable.getName());
        
        // generate an EmbeddableAccessor for this type
        generateEmbeddable(plsqlTable, targetClassName);

        return plsqlTable;
    }
    
    /**
     * Process the given PLSQLRecordType and return a PLSQLRecordMetadata instance.
     * 
     */
    protected PLSQLRecordMetadata processPLSQLRecordType(PLSQLRecordType plsqlRecordType) {
        // for %ROWTYPE we create a 'place holder' PL/SQL Record - in this case there is no package name
        String typeName = getQualifiedTypeName(plsqlRecordType);
        String compatibleName = getQualifiedCompatibleTypeName(plsqlRecordType);
        if (compatibleName.contains(PERCENT)) {
            compatibleName = compatibleName.replace(PERCENT, UNDERSCORE);
        }
        
        PLSQLRecordMetadata plsqlRecord = new PLSQLRecordMetadata();
        plsqlRecord.setName(typeName);
        plsqlRecord.setCompatibleType(compatibleName);
        if (typeName.endsWith(ROWTYPE_STR)) {
            plsqlRecord.setJavaType(getGeneratedJavaClassName(compatibleName));
        } else {
            plsqlRecord.setJavaType(getGeneratedJavaClassName(typeName));
        }

        List<PLSQLParameterMetadata> fields = new ArrayList<PLSQLParameterMetadata>();
        PLSQLParameterMetadata field;
        for (FieldType fld : plsqlRecordType.getFields()) {
            field = new PLSQLParameterMetadata();
            field.setName(fld.getFieldName());
            String dbType = processTypeName(fld.getTypeName());
            if (fld.isComposite()) {
                DatabaseType enclosedType = fld.getEnclosedType();
                // may need to prepend package name
                if (enclosedType.isPLSQLType()) {
                    dbType = ((PLSQLType) fld.getEnclosedType()).getParentType().getPackageName() + DOT + dbType;
                }
                processCompositeType(enclosedType, dbType);
            }
            field.setDatabaseType(dbType);
            fields.add(field);
        }
        plsqlRecord.setFields(fields);
        
        // avoid double-processing
        getProcessedTypes().add(plsqlRecord.getName());
        
        // generate an EmbeddableAccessor for this type
        generateEmbeddable(plsqlRecord, plsqlRecordType);
        
        return plsqlRecord;
    }
    
    /**
     * Process the given composite database type.
     */
    protected void processCompositeType(DatabaseType compositeType) {
        processCompositeType(compositeType, compositeType.getTypeName());
    }
    /**
     * Process the given composite database type.  For PL/SQL types, typeName will be
     * the fully qualified type name (i.e. packagename.typename).  The type should 
     * be one of:  PLSQLCollection, PLSQLRecord, Object, ObjectTable, or Varray.
     */
    protected void processCompositeType(DatabaseType compositeType, String typeName) {
        // avoid double-processing of records & collections, objects and arrays
        if (!alreadyProcessed(typeName)) {
            if (compositeType.isPLSQLCollectionType()) {
                getPlsqlTables().add(processPLSQLCollectionType((PLSQLCollectionType) compositeType));
            } else if (compositeType.isPLSQLRecordType()) {
                getPlsqlRecords().add(processPLSQLRecordType((PLSQLRecordType) compositeType));
            } else if (compositeType.isObjectType()) {
                getObjectTypes().add(processObjectType((ObjectType) compositeType));
            } else if (compositeType.isObjectTableType() || compositeType.isVArrayType()) {
                getArrayTypes().add(processArrayType(compositeType));
            }
        }
    }
    
    /**
     * Generate an Embeddable for the given PLSQLTableMetadata, and add
     * it to the list of Embeddables on the XMLEntityMappings instance.
     * 
     * @param tableMetadata PLSQLTableMetadata used to build the Embeddable
     */
    protected void generateEmbeddable(PLSQLTableMetadata tableMetadata, String targetClassName) {
        // avoid double-processing
        if (!embeddableAlreadyProcessed(tableMetadata.getJavaType())) {
            EmbeddableAccessor embeddable = initEmbeddable(tableMetadata.getJavaType());

            ArrayAccessor array = generateArrayAccessor(ITEMS_FLD_STR, ITEMS_COL_STR, tableMetadata.getCompatibleType(), targetClassName);
            embeddable.getAttributes().getArrays().add(array);
            
            // set on the XMLEntityMappings instance
            xmlEntityMappings.getEmbeddables().add(embeddable);
            
            // track to avoid double processing
            getGeneratedEmbeddables().add(tableMetadata.getJavaType());
        }
    }
    
    /**
     * Generate an Embeddable for the given PLSQLRecordMetadata and PLSQLRecordType, 
     * and add it to the list of Embeddables on the XMLEntityMappings instance.
     * 
     * @param recordMetadata PLSQLRecordMetadata used to build the Embeddable
     * @param recordType PLSQLRecordType used to build the Embeddable
     */
    protected void generateEmbeddable(PLSQLRecordMetadata recordMetadata, PLSQLRecordType recordType) {
        // avoid double-processing
        if (!embeddableAlreadyProcessed(recordMetadata.getJavaType())) {
            EmbeddableAccessor embeddable = initEmbeddable(recordMetadata.getJavaType());
            
            // add a struct to satisfy field ordering
            StructMetadata struct = new StructMetadata();
            struct.setName(recordMetadata.getCompatibleType());
            List<String> fields = new ArrayList<String>();
            for (PLSQLParameterMetadata fld : recordMetadata.getFields()) {
                fields.add(fld.getName());
            }
            struct.setFields(fields);
            embeddable.setStruct(struct);
            
            // add an attribute to the embeddable for each of the record's fields
            addEmbeddableAttributes(embeddable, recordType.getFields());

            // set on the XMLEntityMappings instance
            xmlEntityMappings.getEmbeddables().add(embeddable);
            
            // track to avoid double processing
            getGeneratedEmbeddables().add(recordMetadata.getJavaType());
        }
    }
    
    /**
     * Generate an Embeddable for the given OracleArrayTypeMetadata, and add
     * it to the list of Embeddables on the XMLEntityMappings instance.
     */
    protected void generateEmbeddable(OracleArrayTypeMetadata arrayTypeMetadata, CompositeDatabaseTypeWithEnclosedType dbType) {
        // avoid double-processing
        if (!embeddableAlreadyProcessed(arrayTypeMetadata.getJavaType())) {
            EmbeddableAccessor embeddable = initEmbeddable(arrayTypeMetadata.getJavaType());
            
            ArrayAccessor array; 
            if (dbType.getEnclosedType().isComposite()) {
                array = generateArrayAccessor(ITEMS_FLD_STR, ITEMS_COL_STR, arrayTypeMetadata.getNestedType(), 
                        getGeneratedJavaClassName(arrayTypeMetadata.getNestedType(), defaultPackage));
            } else {
                array = generateArrayAccessor(ITEMS_FLD_STR, ITEMS_COL_STR, dbType.getEnclosedType().getTypeName());
            }
            
            embeddable.getAttributes().getArrays().add(array);
            
            // set on the XMLEntityMappings instance
            xmlEntityMappings.getEmbeddables().add(embeddable);
            
            // track to avoid double processing
            getGeneratedEmbeddables().add(arrayTypeMetadata.getJavaType());
        }
    }
    
    /**
     * Generate an Embeddable for the given OracleObjectTypeMetadata, and add
     * it to the list of Embeddables on the XMLEntityMappings instance.
     */
    protected void generateEmbeddable(OracleObjectTypeMetadata objectTypeMetadata, ObjectType objectType) {
        // avoid double-processing
        if (!embeddableAlreadyProcessed(objectTypeMetadata.getJavaType())) {
            EmbeddableAccessor embeddable = initEmbeddable(objectTypeMetadata.getJavaType());      
            
            // add a struct to satisfy field ordering
            StructMetadata struct = new StructMetadata();
            struct.setName(objectTypeMetadata.getName());
            List<String> fields = new ArrayList<String>();
            for (PLSQLParameterMetadata fld : objectTypeMetadata.getFields()) {
                fields.add(fld.getName());
            }
            struct.setFields(fields);
            embeddable.setStruct(struct);
            
            // add an attribute to the embeddable for each of the object's fields
            addEmbeddableAttributes(embeddable, objectType.getFields());

            // set on the XMLEntityMappings instance
            xmlEntityMappings.getEmbeddables().add(embeddable);
            
            // track to avoid double processing
            getGeneratedEmbeddables().add(objectTypeMetadata.getJavaType());
        }
    }

    /**
     * Convenience method that creates and EmbeddableAccessor, setting the class name to the
     * provided embeddableClassName, initializes the various lists (Basics, Arrays, etc.), 
     * and sets the access type to 'VIRTUAL'.
     */
    protected EmbeddableAccessor initEmbeddable(String embeddableClassName) {
        EmbeddableAccessor embeddable = new EmbeddableAccessor();
        embeddable.setClassName(embeddableClassName);
        embeddable.setAccess(EL_ACCESS_VIRTUAL);
        
        embeddable.setAttributes(new XMLAttributes());
        embeddable.getAttributes().setBasics(new ArrayList<BasicAccessor>());
        embeddable.getAttributes().setArrays(new ArrayList<ArrayAccessor>());
        embeddable.getAttributes().setStructures(new ArrayList<StructureAccessor>());
        embeddable.getAttributes().setEmbeddeds(new ArrayList<EmbeddedAccessor>());
        
        return embeddable;
    }
    
    /**
     * Process a list of FieldTypes, creating an attribute for each - the created 
     * XMLAttributes are set on the given EmbeddableAccessor.
     * 
     * @see org.eclipse.persistence.internal.jpa.metadata.accessors.classes.XMLAttributes
     */
    protected void addEmbeddableAttributes(EmbeddableAccessor embeddable, List<FieldType> fields) {
        for (FieldType fld : fields) {
            DatabaseType enclosedType = fld.getEnclosedType();
            if (!enclosedType.isComposite() || enclosedType.isTYPEType()) {  // basic
                String typeName = enclosedType.isTYPEType() ? ((TYPEType) enclosedType).getTypeName() : fld.getTypeName();
                BasicAccessor basic = generateBasicAccessor(fld.getFieldName().toLowerCase(), fld.getFieldName(), getClassNameFromJDBCTypeName(typeName, dbPlatform));
                embeddable.getAttributes().getBasics().add(basic);
            } else if (enclosedType.isPLSQLType()) {  // record or collection
                PLSQLType plsqlType = (PLSQLType) enclosedType;
                String typeName = getQualifiedTypeName(plsqlType);
                EmbeddedAccessor embedded = new EmbeddedAccessor();
                embedded.setName(fld.getFieldName().toLowerCase());
                embedded.setAttributeType(getGeneratedJavaClassName(typeName));
                embeddable.getAttributes().getEmbeddeds().add(embedded);
            } else if (enclosedType.isVArrayType() || enclosedType.isObjectTableType()) {  // array
                ArrayAccessor array = null;
                // target class is reference class name for Object Table, and structure name for Varray
                if (enclosedType.isVArrayType()) {
                    array = generateArrayAccessor(fld.getFieldName().toLowerCase(), fld.getFieldName(), enclosedType.getTypeName());
                } else {
                    ObjectTableType otType = (ObjectTableType) enclosedType;
                    array = generateArrayAccessor(fld.getFieldName().toLowerCase(), fld.getFieldName(), otType.getEnclosedType().getTypeName(), 
                            getGeneratedJavaClassName(otType.getEnclosedType().getTypeName(), defaultPackage));
                }
                embeddable.getAttributes().getArrays().add(array);
            } else if (enclosedType.isObjectType()) {  // struct
                StructureAccessor structure = generateStructureAccessor(fld.getFieldName().toLowerCase(), fld.getFieldName(), 
                        getGeneratedJavaClassName(enclosedType.getTypeName(), defaultPackage));
                embeddable.getAttributes().getStructures().add(structure);
            } else if (enclosedType.isTYPEType()) {
                TYPEType tType = (TYPEType) enclosedType;
                BasicAccessor basic = generateBasicAccessor(fld.getFieldName().toLowerCase(), fld.getFieldName(), getClassNameFromJDBCTypeName(tType.getTypeName(), dbPlatform));
                embeddable.getAttributes().getBasics().add(basic);                
            }
        }
    }
    
    /**
     * Returns an ArrayAccessor instance, constructed based on the given String values.  This method
     * can be used when the database type and target class names are the same.
     * 
     */
    protected ArrayAccessor generateArrayAccessor(String arrayName, String columnName, String databaseTypeName) {
        return generateArrayAccessor(arrayName, columnName, databaseTypeName, databaseTypeName);
    }
    
    /**
     * Returns an ArrayAccessor instance, constructed based on the given String values.
     */
    protected ArrayAccessor generateArrayAccessor(String arrayName, String columnName, String databaseTypeName, String targetClassName) {
        ArrayAccessor array = new ArrayAccessor();
        array.setName(arrayName);
        array.setAttributeType(ARRAYLIST_STR);
        array.setDatabaseType(databaseTypeName);
        array.setTargetClassName(targetClassName);
        ColumnMetadata column = new ColumnMetadata();
        column.setName(columnName);
        array.setColumn(column);
        return array;
    }
    
    /**
     * Returns a BasicAccessor instance, constructed based on the given String values.
     */
    protected BasicAccessor generateBasicAccessor(String basicName, String columnName, String attributeTypeName) {
        BasicAccessor basic = new BasicAccessor();
        basic.setName(basicName);
        basic.setAttributeType(attributeTypeName);
        
        ColumnMetadata column = new ColumnMetadata();
        column.setName(columnName);
        basic.setColumn(column);
        return basic;
    }
    
    /**
     * Returns a StructureAccessor instance, constructed based on the given String values.  This method can be used 
     * when the attribute type and target class names are the same.
     */
    protected StructureAccessor generateStructureAccessor(String structureName, String columnName, String attributeTypeName) {
        return generateStructureAccessor(structureName, columnName, attributeTypeName, attributeTypeName);
    }
    
    /**
     * Returns a StructureAccessor instance, constructed based on the given String values.
     */
    protected StructureAccessor generateStructureAccessor(String structureName, String columnName, String attributeTypeName, String targetClassName) {
        StructureAccessor structure = new StructureAccessor();
        structure.setName(structureName);
        structure.setAttributeType(attributeTypeName);
        structure.setTargetClassName(targetClassName);
        ColumnMetadata column = new ColumnMetadata();
        column.setName(columnName);
        structure.setColumn(column);
        return structure;
    }
    
    /**
     * Lazy-load the List of PLSQLRecordMetadata.
     */
    protected List<PLSQLRecordMetadata> getPlsqlRecords() {
        if (plsqlRecords == null) {
            plsqlRecords = new ArrayList<PLSQLRecordMetadata>();
        }
        return plsqlRecords;
    }
    /**
     * Lazy-load the List of PLSQLTableMetadata.
     */
    protected List<PLSQLTableMetadata> getPlsqlTables() {
        if (plsqlTables == null) {
            plsqlTables = new ArrayList<PLSQLTableMetadata>();
        }
        return plsqlTables;
    }
    /**
     * Lazy-load the List of OracleObjectTypeMetadata.
     */
    protected List<OracleObjectTypeMetadata> getObjectTypes() {
        if (objectTypes == null) {
            objectTypes = new ArrayList<OracleObjectTypeMetadata>();
        }
        return objectTypes;
    }
    /**
     * Lazy-load the List of OracleArrayTypeMetadata.
     */
    protected List<OracleArrayTypeMetadata> getArrayTypes() {
        if (arrayTypes == null) {
            arrayTypes = new ArrayList<OracleArrayTypeMetadata>();
        }
        return arrayTypes;
    }
    /**
     * Lazy-load the List of NamedPLSQLStoredProcedureQueryMetadata.
     */
    protected List<NamedPLSQLStoredProcedureQueryMetadata> getPlsqlStoredProcs() {
        if (plsqlStoredProcs == null) {
            plsqlStoredProcs = new ArrayList<NamedPLSQLStoredProcedureQueryMetadata>();
        }
        return plsqlStoredProcs;
    }
    /**
     * Lazy-load the List of NamedPLSQLStoredFunctionQueryMetadata.
     */
    protected List<NamedPLSQLStoredFunctionQueryMetadata> getPlsqlStoredFuncs() {
        if (plsqlStoredFuncs == null) {
            plsqlStoredFuncs = new ArrayList<NamedPLSQLStoredFunctionQueryMetadata>();
        }
        return plsqlStoredFuncs;
    }
    /**
     * Lazy-load the List of NamedStoredProcedureQueryMetadata.
     */
    protected List<NamedStoredProcedureQueryMetadata> getStoredProcs() {
        if (storedProcs == null) {
            storedProcs = new ArrayList<NamedStoredProcedureQueryMetadata>();
        }
        return storedProcs;
    }
    /**
     * Lazy-load the List of NamedStoredFunctionQueryMetadata.
     */
    protected List<NamedStoredFunctionQueryMetadata> getStoredFuncs() {
        if (storedFuncs == null) {
            storedFuncs = new ArrayList<NamedStoredFunctionQueryMetadata>();
        }
        return storedFuncs;
    }
    /**
     * Lazy-load the List of NamedNativeQueryMetadata.
     */
    protected List<NamedNativeQueryMetadata> getNamedNativeQueries() {
        if (namedNativeQueries == null) {
            namedNativeQueries = new ArrayList<NamedNativeQueryMetadata>();
        }
        return namedNativeQueries;
    }
    /**
     * Lazy-load the List of processed composite types.
     */
    protected List<String> getProcessedTypes() {
        if (processedTypes == null) {
            processedTypes = new ArrayList<String>();
        }
        return processedTypes;
    }
    /**
     * Lazy-load the List of embeddables.
     */
    protected List<String> getGeneratedEmbeddables() {
        if (generatedEmbeddables == null) {
            generatedEmbeddables = new ArrayList<String>();
        }
        return generatedEmbeddables;
    }
    
    /**
     * Indicates if an embeddable has already been processed - the list of
     * generated embeddable names will be checked for the given typeName.
     */
    protected boolean embeddableAlreadyProcessed(String embeddableName) {
        return generatedEmbeddables != null && generatedEmbeddables.size() > 0 && generatedEmbeddables.contains(embeddableName);
    }
    
    /**
     * Indicates if a type has already been processed - the list of
     * processed type names will be checked for the given typeName.
     */
    protected boolean alreadyProcessed(String typeName) {
        return processedTypes != null && processedTypes.size() > 0 && processedTypes.contains(typeName);
    }
    

    /**
     * Convenience method that will check each of the lists of generated metadata
     * and add them to the XMLEntityMappings instance if non-null.
     */
    protected void applyGeneratedMetadata() {
        if (storedFuncs != null) {
            xmlEntityMappings.setNamedStoredFunctionQueries(storedFuncs);
        }        
        if (storedProcs != null) {
            xmlEntityMappings.setNamedStoredProcedureQueries(storedProcs);
        }
        if (objectTypes != null) { 
            xmlEntityMappings.setOracleObjectTypes(objectTypes);
        }
        if (arrayTypes != null) { 
            xmlEntityMappings.setOracleArrayTypes(arrayTypes);
        }
        if (plsqlStoredFuncs != null) {
            xmlEntityMappings.setNamedPLSQLStoredFunctionQueries(plsqlStoredFuncs);
        }
        if (plsqlStoredProcs != null) {
            xmlEntityMappings.setNamedPLSQLStoredProcedureQueries(plsqlStoredProcs);
        }
        if (plsqlTables != null) {
            xmlEntityMappings.setPLSQLTables(plsqlTables);
        }
        if (plsqlRecords != null) {
            xmlEntityMappings.setPLSQLRecords(plsqlRecords);
        }
    }
    
    /**
     * Attempt to load the DatabasePlatform using the given platform class name.  If the
     * platform cannot be loaded Oracle11Platform will be returned - if available.
     * 
     * @param platformClassName class name of the DatabasePlatform to be loaded
     * @return DatabasePlatform loaded for the given platformClassname, or Oracle11Platform if not found
     * @see org.eclipse.persistence.platform.database.oracle.Oracle11Platform
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static DatabasePlatform loadDatabasePlatform(String platformClassName) {
        DatabasePlatform dbPlatform = null;
        Class platformClass = null;
        try {
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                platformClass = (Class)AccessController.doPrivileged(new PrivilegedClassForName(platformClassName));
            } else {
                platformClass = PrivilegedAccessHelper.getClassForName(platformClassName);
            }
            dbPlatform = (DatabasePlatform) Helper.getInstanceFromClass(platformClass);
        } catch (Exception e) {
            try {
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    platformClass = (Class) AccessController.doPrivileged(new PrivilegedClassForName(DEFAULT_PLATFORM));
                } else {
                    platformClass = PrivilegedAccessHelper.getClassForName(DEFAULT_PLATFORM);
                }
                dbPlatform = (DatabasePlatform) Helper.getInstanceFromClass(platformClass);
            } catch (Exception ex) {
                // at this point we can't load the default Oracle11 platform, so null will be returned
            }
        }
        return dbPlatform;
    }
}