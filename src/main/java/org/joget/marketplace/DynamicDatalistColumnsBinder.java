package org.joget.marketplace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListBinder;
import org.joget.apps.datalist.model.DataListBinderDefault;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

public class DynamicDatalistColumnsBinder extends DataListBinderDefault {
    protected DataListBinder binder;
    
    public String getName() {
        return "Dynamic Datalist Columns Processor";
    }

    public String getVersion() {
        return "7.0.0";
    }

    public String getDescription() {
        return "A datalist binder wrapper to generate datalist with dynamic columns.";
    }

    public DataListColumn[] getColumns() {
        return getBinder().getColumns();
    }

    public String getPrimaryKeyColumnName() {
        return getBinder().getPrimaryKeyColumnName();
    }

    public DataListCollection getData(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        String bypassProcessing = (String)properties.get("bypass");
        if(!bypassProcessing.equalsIgnoreCase("true")){
            processColumns(dataList);
        }
        
        return getBinder().getData(dataList, getBinder().getProperties(), filterQueryObjects, sort, desc, start, rows);
    }

    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        return getBinder().getDataTotalRowCount(dataList, getBinder().getProperties(), filterQueryObjects);
    }

    public String getLabel() {
        return "Dynamic Datalist Columns Processor";
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/dynamicDatalistColumnsProcessor.json", null, true, "message/datalist/dynamicDatalistColumnsProcessor");
    }
    
    protected Object getObject(ResultSet rs, String mapping){
        try {
            Integer index = null;
            try {
                index = Integer.parseInt(mapping);
            } catch (Exception e) {}
            if (index != null) {
                return rs.getObject(index);
            } else {
                return rs.getObject(mapping);
            }
        } catch (Exception e) {
            return "";
        }
    }

    protected void processColumns(DataList dataList) {
        DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        
        DataListColumn[] columns = dataList.getColumns();
        
        Collection<DataListColumn> newColumns = new ArrayList<DataListColumn>();
        
        try {
            con = ds.getConnection();
            pstmt = con.prepareStatement(getPropertyString("sql"));
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String columnId = (String) getObject(rs, getPropertyString("mp_column_id"));
                if(!columnId.isEmpty()){
                    for(DataListColumn column : columns){
                        if(column.getProperty("id").toString().equalsIgnoreCase(columnId)){
                            newColumns.add(column);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, null);
        }finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch(Exception e) {
            }
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch(Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch(Exception e) {
            }
        }
        dataList.setColumns(newColumns.toArray(new DataListColumn[]{}));
    }
    
    protected DataListBinder getBinder() {
        if (binder == null) {
            //get the binder
            Object binderData = getProperty("actual_databinder");
            if (binderData != null && binderData instanceof Map) {
                Map bdMap = (Map) binderData;
                if (bdMap != null && bdMap.containsKey("className") && !bdMap.get("className").toString().isEmpty()) {
                    PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
                    binder = (DataListBinder) pluginManager.getPlugin(bdMap.get("className").toString());
                    
                    if (binder != null) {
                        Map bdProps = (Map) bdMap.get("properties");
                        binder.setProperties(bdProps);
                    }
                }
            }
        }
        return binder;
    }
}
