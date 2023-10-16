package com.bobochang.warehouse.aop;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.bobochang.warehouse.annotation.BusLog;
import com.bobochang.warehouse.entity.BusLogDao;
import com.bobochang.warehouse.service.impl.BusLogServiceImpl;
import com.bobochang.warehouse.utils.TokenUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.el.parser.Token;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Aspect
@Slf4j
public class BusLogAop implements Ordered {
    @Autowired
    private BusLogServiceImpl busLogService;


    public static final String path = "D:/project/warehouse/warehouse-backend/log/";


    /**
     * 定义BusLogAop的切入点为标记@BusLog注解的方法
     */
    @Pointcut(value = "@annotation(com.bobochang.warehouse.annotation.BusLog)")
    public void pointcut() {
    }

    /**
     * 业务操作环绕通知
     *
     * @param proceedingJoinPoint
     * @retur
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) {
        log.info("----BusAop 环绕通知 start");
        // 获取方法参数
        Object[] args = proceedingJoinPoint.getArgs();

        // 寻找operPerson的值
        String operPerson = null;
        for (Object arg : args) {
            if (arg instanceof String) {
                operPerson = (String) arg;
                break;
            }
        }
        //执行目标方法
        Object result = null;
        try {
            result = proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        //目标方法执行完成后，获取目标类、目标方法上的业务日志注解上的功能名称和功能描述
        Object target = proceedingJoinPoint.getTarget();
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        BusLog anno1 = target.getClass().getAnnotation(BusLog.class);
        BusLog anno2 = signature.getMethod().getAnnotation(BusLog.class);
        BusLogDao busLogBean = new BusLogDao();
        String logName = anno1.name();
        String logDescrip = anno2.descrip();
        busLogBean.setBusName(logName);
        busLogBean.setBusDescrip(logDescrip);
        busLogBean.setOperPerson(operPerson);
        busLogBean.setOperTime(new Date());
        String text = operPerson + "-" + logName + "-" + new Date();
        //把参数报文写入到文件中
        OutputStream outputStream = null;
        String logFolder = path + logName;
        String logFilePath = logFolder + File.separator + DateUtil.format(new Date(), DatePattern.PURE_DATE_FORMATTER) + ".log";

        File folder = new File(logFolder);
        File file = new File(logFilePath);

        try {
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    log.info("文件夹已创建: " + logFolder);
                } else {
                    log.error("文件夹创建失败: " + logFolder);
                }
            }

            if (!file.exists()) {
                if (file.createNewFile()) {
                    log.info("文件已创建: " + logFilePath);
                } else {
                    log.error("文件创建失败: " + logFilePath);
                }
            }

            // 在text末尾添加换行符
            String textWithNewLine = text + "\n";

            // 使用FileOutputStream追加内容
            outputStream = new FileOutputStream(file, true);
            outputStream.write(textWithNewLine.getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        busLogService.insert(busLogBean);
        log.info("----BusAop 环绕通知 end");
        return result;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
