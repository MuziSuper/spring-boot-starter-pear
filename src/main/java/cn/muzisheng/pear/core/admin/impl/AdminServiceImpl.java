package cn.muzisheng.pear.core.admin.impl;

import cn.muzisheng.pear.constant.Constant;
import cn.muzisheng.pear.core.admin.AdminService;
import cn.muzisheng.pear.exception.GeneralException;
import cn.muzisheng.pear.mapper.AdminMapper;
import cn.muzisheng.pear.model.*;
import cn.muzisheng.pear.params.AdminQueryResult;
import cn.muzisheng.pear.params.Filter;
import cn.muzisheng.pear.params.QueryForm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminServiceImpl<T> implements AdminService {

    private final AdminMapper<T> adminMapper;
    private static final Logger LOG = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    public AdminServiceImpl(AdminMapper<T> adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Override
    public ResponseEntity<Result<Object>> handleQueryOrGetOne(HttpServletRequest request, String model, AdminObject<T> adminObject, QueryForm queryForm) {
        if (request.getContentLength() <= 0) {
            return handleGetOne(request, model, adminObject, queryForm);
        }
        queryForm.defaultPrepareQuery();
        if (queryForm.isForeignMode()) {
            queryForm.setLimit(0);
        }
        AdminQueryResult result = queryObjects(request, model, adminObject, form);

    }


    @Override
    public ResponseEntity<Result<Map<String, Object>>> handleCreate(AdminObject adminObject) {
        return null;
    }

    @Override
    public ResponseEntity<Result<Map<String, Object>>> handleUpdate(AdminObject adminObject) {
        return null;
    }

    @Override
    public ResponseEntity<Result<Map<String, Object>>> handleDelete(AdminObject adminObject) {
        return null;
    }

    @Override
    public ResponseEntity<Result<Map<String, Object>>> handleAction(AdminObject adminObject) {
        return null;
    }

    /**
     * 拼接数据库db的查询子句,筛选子句，排序子句，获取查询总数，
     * 携带外键连接的表其所有数据，将多条总数据放入映射集合与列表中，
     * 封装在r AdminQueryResult中
     **/
    private AdminQueryResult queryObjects(HttpServletRequest request, String model, AdminObject adminObject, QueryForm queryForm) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        for (Filter filter : adminObject.getFilters()) {
            getQuery(filter, wrapper);
            if (Constant.FILTER_OP_LIKE.equals(filter.getOp())){
                if (filter.getValue() instanceof Object[] values) {
                    wrapper.and(w->{
                        for(Object value : values){
                            if (value instanceof String) {
                                w.like(filter.getName(), value);
                            } else {
                                // 处理 value 不是 String 的情况
                                throw new GeneralException("Expected a String value for like operation but got " + value.getClass().getName());
                            }
                        }
                    });
                }else{
                    wrapper.like(filter.getName(), filter.getValue());
                }
            } else if (Constant.FILTER_OP_BETWEEN.equals(filter.getOp())) {
                if (filter.getValue() instanceof Object[] values) {
                    if(values.length == 2){
                        wrapper.between(filter.getName(), values[0], values[1]);
                    }else{
                        throw new GeneralException("Expected an Object[] value with length 2 for between operation but got " + values.length);
                    }
                }else{
                    throw new GeneralException("Expected an Object[] value for between operation but got " + filter.getValue().getClass().getName());
                }
            }
        }
    }
    /**
     * 拼接数据库db的查询子句,Like与Between除外
     **/
    private QueryWrapper<T> getQuery(Filter filter, QueryWrapper<T> queryWrapper) {
        switch (filter.getOp()) {
            case Constant.FILTER_OP_IS_NOT:
                if (filter.getValue() == null) {
                    queryWrapper.isNull(filter.getName());
                } else {
                    queryWrapper.ne(filter.getName(), filter.getValue());
                }
            case Constant.FILTER_OP_EQUAL:
                queryWrapper.eq(filter.getName(), filter.getValue());
                break;
            case Constant.FILTER_OP_NOT_EQUAL:
                queryWrapper.ne(filter.getName(), filter.getValue());
                break;
            case Constant.FILTER_OP_IN:
                queryWrapper.in(filter.getName(), filter.getValue());
                break;
            case Constant.FILTER_OP_NOT_IN:
                queryWrapper.notIn(filter.getName(), filter.getValue());
                break;
            case Constant.FILTER_OP_GREATER:
                queryWrapper.gt(filter.getName(), filter.getValue());
                break;
            case Constant.FILTER_OP_GREATER_OR_EQUAL:
                queryWrapper.ge(filter.getName(), filter.getValue());
                break;
            case Constant.FILTER_OP_LESS:
                queryWrapper.lt(filter.getName(), filter.getValue());
                break;
            case Constant.FILTER_OP_LESS_OR_EQUAL:
                queryWrapper.le(filter.getName(), filter.getValue());
                break;
            default:
                break;
        }
        return queryWrapper;
    }

    /**
     * 客户端通过url的参数进行数据查询 (不通过请求体传入参数)
     **/
    private ResponseEntity<Result<Object>> handleGetOne(HttpServletRequest request, String model, AdminObject adminObject, QueryForm queryForm) {
        Response<Object> response = new Response<>();
        Map<String, Object> queryMap = getPrimaryValues(request, model, adminObject, queryForm);
        if (queryMap.isEmpty()) {
            LOG.error("invalid primary key");
            throw new GeneralException("invalid primary key");
        }
        Object result = adminMapper.selectByMap(queryMap).getFirst();
        if (result == null) {
            LOG.error("Data cannot be found.");
            throw new GeneralException("Data cannot be found.");
        }
        if (adminObject.getBeforeRender() != null) {
            Object res;
            try {
                res = adminObject.getBeforeRender().execute(request, result);
            } catch (Exception e) {
                LOG.error("BeforeRender error");
                throw new GeneralException("BeforeRender error");
            }
            if (res != null) {
                result = res;
            }
//            Class<?> type =result.getClass();
//            if(res != null) {
//                if (type.isInstance(res)) {
//                    result = (T) res;
//                } else {
//                    // 处理类型不匹配的情况
//                    throw new GeneralException("res cannot be cast to " + type.getName());
//                }
//            }
        }
        Map<String, Object> data = marshalOne(request, adminObject, result);
        response.setData(data);
        return response.value();
    }

    /**
     * 序列化对象，将result转为map[string]any
     **/
    private Map<String, Object> marshalOne(HttpServletRequest request, AdminObject adminObject, Object result) {
        Map<String, Object> data = new HashMap<>();
        for (AdminField field : adminObject.getFields()) {
            if (field.getForeign() != null) {
                AdminValue foreignValue = new AdminValue();
                foreignValue.setValue(field.getForeign().getForeignKey());
                if (field.getForeign().getFieldName() != null) {
                    foreignValue.setLabel(field.getForeign().getFieldName());
                } else {
                    foreignValue.setLabel(foreignValue.getValue().toString());
                }
                data.put(field.getName(), foreignValue);
            } else {
                String filedName = field.getName();
                data.put(field.getName(), filedName);
            }
        }
        if (adminObject.getAdminViewOnSite() != null) {
            try {
                data.put("_adminExtra", adminObject.getAdminViewOnSite().execute(request, result));
            } catch (Exception e) {
                LOG.error("AdminViewOnSite error");
            }
        }
        return data;
    }

    /**
     * 根据query值遍历并获取其映射数据库表的主键或外键键值对
     **/
    private Map<String, Object> getPrimaryValues(HttpServletRequest request, String model, AdminObject adminObject, QueryForm queryForm) {
        Map<String, Object> queryMap = new HashMap<>();
        boolean keysExist = false;
        for (String field : adminObject.getPrimaryKeys()) {
            String param = request.getParameter(field);
            if (!param.isEmpty()) {
                queryMap.put(field, param);
                keysExist = true;
            }
        }
        if (keysExist) {
            return queryMap;
        }
        for (String field : adminObject.getUniqueKeys()) {
            String param = request.getParameter(field);
            if (!param.isEmpty()) {
                queryMap.put(field, param);
            }
        }
        return queryMap;
    }

}
