package com.bobochang.warehouse.service;

import com.bobochang.warehouse.entity.Flow;
import com.bobochang.warehouse.entity.Result;

import java.util.List;
import java.util.Map;

/**
 * 工作流服务类
 */
public interface ActivitiService {
    // 上传流程定义的xml文件
    public String xmlUpload(String file,String fileName);

    // 查看用户是否有任务
    public Result haveTask(int userId);

    // 开启流程实例
    Result startInstance(Map<String, String> map);

    // 完成流程实例中的任务
    public void completeTask(String assignee, Flow flow);

    // 查看用户所有流程实例的任务
    List<Object> searchTask(String roleCode);
}
