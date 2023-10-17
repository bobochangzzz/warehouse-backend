package com.bobochang.warehouse.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.hash.Hash;
import com.bobochang.warehouse.config.SystemConstants;
import com.bobochang.warehouse.constants.WarehouseConstants;
import com.bobochang.warehouse.dto.PageCheckinDto;
import com.bobochang.warehouse.entity.Checkin;
import com.bobochang.warehouse.entity.CurrentUser;
import com.bobochang.warehouse.entity.Result;
import com.bobochang.warehouse.entity.User;
import com.bobochang.warehouse.page.Page;
import com.bobochang.warehouse.service.CheckinService;
import com.bobochang.warehouse.service.UserService;
import com.bobochang.warehouse.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;

@RequestMapping("/checkin")
@RestController
@Slf4j
public class CheckinController {
    @Autowired
    private CheckinService checkinService;

    @Value("${file.image-folder}")
    private String imageFolder;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemConstants constants;

    @Autowired
    private TokenUtils tokenUtils;

    /**
     * 验证用户是否能够签到
     * @param token
     * @return
     */
    @GetMapping("/validCanCheckIn")
    public Result validCanCheckIn(@RequestHeader(WarehouseConstants.HEADER_TOKEN_NAME) String token){
        int userId = getUserIdByToken(token);
        log.info("验证签到");
        String result=checkinService.validCanCheckIn(userId, DateUtil.today());
        return Result.ok(result);
    }

    /**
     * 签到
     * @param file
     * @return
     */
    @PostMapping("/checkin")
    public Result checkin(@RequestParam("photo") MultipartFile file,@RequestHeader(WarehouseConstants.HEADER_TOKEN_NAME) String token){
        log.info("请求签到");
        if(file==null){
            return Result.err(500,"没有上传文件");
        }
        int userId= getUserIdByToken(token);
        String fileName=file.getOriginalFilename().toLowerCase();
        if(!fileName.endsWith(".jpg")){
            return Result.err(500,"必须提交jpg图片");
        }
        else{
            String path=imageFolder+"/"+fileName;
            System.out.println(fileName);
            try{
                // 将上传的文件复制到对应的文件夹里面
                file.transferTo(Paths.get(path));
                HashMap param=new HashMap();
                param.put("userId",userId);
                param.put("path",path);
                return checkinService.checkin(param);
            }catch (IOException e){
                log.error(e.getMessage(),e);
                return Result.err(500,e.getMessage());
            }
            finally {
                FileUtil.del(path);
            }
        }
    }

    /**
     * 查看是否有人脸模型
     * @param user
     * @return
     */
    @GetMapping("/haveFace")
    public Result haveFace(User user) {
        String result = checkinService.haceFace(user.getUserId());
        return Result.ok(result);
    }

    /**
     * 签到成功后返回的签到信息
     * @param
     * @return
     */
    @GetMapping("/searchTodayCheckin")
    public Result searchTodayCheckin(@RequestHeader(WarehouseConstants.HEADER_TOKEN_NAME) String token){
        int userId=getUserIdByToken(token);
        // 查看当天的签到记录，包括签到的开始时间和结束时间
        HashMap map=checkinService.searchTodayCheckin(userId);
        map.put("attendanceTime",constants.attendanceTime);
        map.put("closingTime",constants.closingTime);

        // 查看用户签到的所有日期
        long days=checkinService.searchCheckinDaysCount(userId);
        map.put("checkinDays",days);

        // 判断这一周的开始日期是否早于入职日期，是的话就换成入职日期
        DateTime hiredate=DateUtil.parse(userService.searchUserHiredate(userId));
        DateTime startDate=DateUtil.beginOfWeek(DateUtil.date());
        if(startDate.isBefore(hiredate)){
            startDate=hiredate;
        }

        // 获取当天所在周的所有签到记录
        DateTime endDate=DateUtil.endOfWeek(DateUtil.date());
        HashMap param=new HashMap();
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        param.put("userId",userId);
        ArrayList<HashMap> list=checkinService.searchWeekCheckin(param);
        map.put("weekCheckin",list);
        return Result.ok(map);
    }

    /**
     * 查询所有签到记录
     * @param user
     * @return
     */
    @GetMapping("/searchAllCheckin")
    public Result searchAllCheckin(User user){
        // 查看用户签到的所有日期以及签到总数
        return checkinService.searchCheckinDays(user.getUserId());
    }

    /**
     * 查询一个星期内的签到记录
     * @param user
     * @return
     */
    @GetMapping("/searchWeekCheckin")
    public Result searchWeekCheckin(User user){
        // 判断这一周的开始日期是否早于入职日期，是的话就换成入职日期
        DateTime hiredate=DateUtil.parse(userService.searchUserHiredate(user.getUserId()));
        DateTime startDate=DateUtil.beginOfWeek(DateUtil.date());
        if(startDate.isBefore(hiredate)){
            startDate=hiredate;
        }

        // 获取当天所在周的所有签到记录
        DateTime endDate=DateUtil.endOfWeek(DateUtil.date());
        HashMap param=new HashMap();
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        param.put("userId",user.getUserId());
        ArrayList<HashMap> list=checkinService.searchWeekCheckin(param);

        return Result.ok(list);
    }

    /**
     * 查询一个月内的签到记录
     * @param form
     * @return
     */
    @GetMapping("/searchMonthCheckin")
    public Result searchMonthCheckin(PageCheckinDto form,@RequestHeader(WarehouseConstants.HEADER_TOKEN_NAME) String token){
        int userId=getUserIdByToken(token);
        // 查询用户的入职日期，如果是开始时间早于入职日期之间则返回错误的数据
        // 并且把开始时间调整到入职日期
        DateTime hiredate=DateUtil.parse(userService.searchUserHiredate(userId));
        String month=form.getMonth()<10?"0"+form.getMonth():form.getMonth().toString();
        DateTime startDate=DateUtil.parse(form.getYear()+"-"+month+"-01");
        // 判断要查询的月份是否在入职日期的月份之后
        if(startDate.isBefore(DateUtil.beginOfMonth(hiredate))){
            return Result.err(500,"入职前无数据");
        }
        // 判断开始日期是否在入职日期之前
        if(startDate.isBefore(hiredate)){
            startDate=hiredate;
        }

        // 获取到开始日期和结束日期就采用查询当周考勤情况的方法
        // 当周考勤情况的方法也是可以查询一段时间内考勤情况的方法
        DateTime endDate=DateUtil.endOfMonth(startDate);
        HashMap param=new HashMap();
        param.put("userId",userId);
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        ArrayList<HashMap> list=checkinService.searchMonthCheckin(param);

        int sum_1=0,sum_2=0,sum_3=0;
        for(HashMap<String,String> one:list){
            String type=one.get("type");
            String status=one.get("status");
            if("工作日".equals(type)){
                if("正常".equals(status)){
                    sum_1++;
                }
                else if("迟到".equals(status)){
                    sum_2++;
                }
                else if("缺勤".equals(status)){
                    sum_3++;
                }
            }
            if("节假日".equals(type)){

            }
        }

        HashMap map = new HashMap<>();
        map.put("list",list);
        map.put("sum_1",sum_1);
        map.put("sum_2",sum_2);
        map.put("sum_3",sum_3);
        return Result.ok(map);
    }

    /**
     * 分页查询
     * @param page
     * @return
     */
    @GetMapping("/checkin-page-list")
    public Result checkinPageList(PageCheckinDto page, Checkin checkin){
        log.info("开始日期",page.getStartDate());
//        log.info(String.valueOf(page.getLimitIndex()));

        page = checkinService.queryChekinPage(page,checkin);
        return Result.ok("page",page);
    }

    /**
     * 查询当月缺勤日期
     * @param page
     * @return
     */
    @GetMapping("/check-date")
    public Result checkDate(PageCheckinDto page){
        LocalDate currentDate = LocalDate.now();
        LocalDate monthStartDate = currentDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEndDate = currentDate.with(TemporalAdjusters.lastDayOfMonth());
        System.out.println(monthEndDate);
        System.out.println(monthStartDate);

        // 查询用户的入职日期，如果是开始时间早于入职日期之间则返回错误的数据
        // 并且把开始时间调整到入职日期

        // 获取到开始日期和结束日期就采用查询当周考勤情况的方法
        // 当周考勤情况的方法也是可以查询一段时间内考勤情况的方法
        HashMap param=new HashMap();
        param.put("userId",page.getUserId());
        param.put("startDate",monthStartDate.toString());
        param.put("endDate",monthEndDate.toString());
        ArrayList<HashMap> list=checkinService.searchMonthCheckin(param);
        System.out.println(list);
        return Result.ok(list);
    }

    /**
     * 根据token获得userId
     * @param token
     * @return
     */
    private int getUserIdByToken(String token){
        CurrentUser currentUser = tokenUtils.getCurrentUser(token);
        //获取当前登录的用户id -- 修改用户的用户id
        int userId = currentUser.getUserId();
        return userId;
    }


}
