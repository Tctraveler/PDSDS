package com.Tc_traveler.PDSDS.controller;

import com.Tc_traveler.PDSDS.dto.Result;
import com.Tc_traveler.PDSDS.entity.Consequence;
import com.Tc_traveler.PDSDS.entity.Doctor;
import com.Tc_traveler.PDSDS.entity.Mail;
import com.Tc_traveler.PDSDS.entity.Patient;
import com.Tc_traveler.PDSDS.service.UserService;
import com.Tc_traveler.PDSDS.utils.JwtUtil;
import com.Tc_traveler.PDSDS.utils.Md5Util;
import com.Tc_traveler.PDSDS.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/doctor")
public class DoctorController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^\\S{1,15}$") String username, @Pattern(regexp = "^\\S{8,32}$") String password){
        Doctor doctor = userService.findByDoctorName(username);
        Doctor doctor_1 = userService.findByDoctor_1Name(username);
        if(doctor == null&&doctor_1==null){
            userService.registerDoctor(username,password);
            return Result.success();
        }else {
            return Result.error("用户名已经被使用!");
        }
    }

    @PostMapping("/login")
    public Result<String> login(String username,String password){
        Doctor doctor = userService.findByDoctorName(username);
        Doctor doctor_1 = userService.findByDoctor_1Name(username);
        if(doctor == null&&doctor_1 != null){
            return Result.error("您的注册尚未通过");
        }
        if(doctor == null){
            return Result.error("用户名不存在");
        }
        if(Md5Util.getMD5String(password).equals(doctor.getPassword())){
            Map<String,Object> claims = new HashMap<>();
            claims.put("id",doctor.getId());
            claims.put("username",doctor.getUsername());
            claims.put("security",doctor.getClass().getSimpleName());
            String token = JwtUtil.genToken(claims);
            return Result.success(token);
        }else {
            return Result.error("密码错误!");
        }
    }

    @GetMapping("/myPatientsInfo")
    public Result<List<Patient>> myPatientsInfo(/*@RequestHeader(name = "Authorization")String token*/){
        Map<String,Object> map = ThreadLocalUtil.get();
        String security = (String) map.get("security");
        int id = (int) map.get("id");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        List<Patient> patients = userService.myPatientsInfo(id);
        return Result.success(patients);
    }

    @GetMapping("/patientDetail")
    public Result<List<Object>> patientDetail(@Pattern(regexp = "^\\S{1,15}$")String username){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        Integer doctor_id = (Integer) claims.get("id");
        Patient patient = userService.findPatientByUsernameAndDoctorId(username,doctor_id);
        if(patient==null){
            return Result.error("您名下没有此病人");
        }
        ArrayList<Object> patientDetail = new ArrayList<>();
        int patient_id = patient.getId();
        patientDetail.add(patient);
        try {
            patientDetail.add(userService.findSDSByPatientId(patient_id).build());
            patientDetail.add(userService.findCES_DByPatientId(patient_id).build());
            patientDetail.add(userService.findMADRSByPatientId(patient_id).build());
        }catch (Exception e){
            e.printStackTrace();
            return Result.error("程序出现内部错误");
        }
        patientDetail.add(userService.findConsequenceByPatientId(patient_id));
        return Result.success(patientDetail);
    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated Doctor doctor){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        userService.update(doctor);
        return Result.success();
    }

    @PatchMapping("/updateDoctorAvatar")
    public Result updateDoctorAvatar(@RequestParam @URL String avatarUrl){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        userService.updateDoctorAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updateDoctorPwd")
    public Result updateDoctorPwd(@RequestBody Map<String,Object> params){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        String oldPwd = (String) params.get("oldPwd");
        String newPwd = (String) params.get("newPwd");
        String rePwd = (String) params.get("rePwd");
        if(!StringUtils.hasLength(oldPwd)||!StringUtils.hasLength(newPwd)||!StringUtils.hasLength(rePwd)){
            return Result.error("缺少必要的参数");
        }
        String username = (String) claims.get("username");
        Doctor doctor = userService.findByDoctorName(username);
        if(!doctor.getPassword().equals(Md5Util.getMD5String(oldPwd))){
            return Result.error("原密码不正确!");
        }
        if (!rePwd.equals(newPwd)){
            return Result.error("两次输入的新密码不相同");
        }
        userService.updateDoctorPwd(newPwd);
        return Result.success();
    }

    @DeleteMapping("/deletePatient")
    public Result deletePatient(@Pattern(regexp = "^\\S{1,15}$") String username){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        Integer doctor_id = (Integer) claims.get("id");
        Patient patient = userService.findPatientByUsernameAndDoctorId(username,doctor_id);
        if(patient==null){
            return Result.error("您名下没有此病人");
        }
        userService.deletePatient(username,doctor_id);
        return Result.success();
    }

    @GetMapping("/getLastPatient")
    public Result<List<Patient>> getLastPatient(){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        List<Patient> patients = userService.getLastPatient();
        return Result.success(patients);
    }

    @PatchMapping("/choosePatient")
    public Result choosePatient(@Pattern(regexp = "^\\S{1,15}$")String username){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        Integer doctor_id = (Integer) claims.get("id");
        Patient patient = userService.findByPatientName(username);
        if(patient==null){
            return Result.error("数据库中没有此病人");
        }
        patient.setDoctor_id(doctor_id);
        userService.choosePatient(patient);
        return Result.success();
    }

    @PostMapping("/addOrder")
    public Result addOrder(@RequestBody Map<String,Object> params){
        Map<String,Object> claims = ThreadLocalUtil.get();
        String security = (String) claims.get("security");
        if(!security.equals("Doctor")){
            return Result.error("您没有足够的权限访问");
        }
        String username = (String) params.get("username");
        String order = (String) params.get("order");
        Integer doctor_id = (Integer) claims.get("id");
        Patient patient = userService.findPatientByUsernameAndDoctorId(username,doctor_id);
        if(patient==null){
            return Result.error("您名下没有此患者");
        }
        Consequence consequence = userService.findConsequenceByPatientId(patient.getId());
        if(consequence!=null){
            userService.updateOrder(patient.getId(),order);
        }
        userService.addOrder(patient.getId(),order);
        return Result.success();
    }

    @PatchMapping("/resetPwd")
    public Result resetPwd(@RequestBody Map<String,Object> params){
        String email = (String) params.get("email");
        String token_1 = (String) params.get("token");
        String newPwd = (String) params.get("newPwd");
        String rePwd = (String) params.get("rePwd");
        Integer token = Integer.parseInt(token_1);
        if(!newPwd.equals(rePwd)){
            return Result.error("两次输入的密码不一样!");
        }
        Mail mail = userService.findMailByEmail(email);
        if(mail == null){
            return Result.error("您还未发送过验证码");
        }
        userService.deleteMail(email);
        LocalDateTime createTime = mail.getCreateTime();
        if(createTime.plusMinutes(5L).isBefore(LocalDateTime.now())){
            return Result.error("验证码已经过期!请重新发送");
        }
        if(!token.equals(mail.getToken())){
            return Result.error("验证码错误！请重新发送");
        }
        userService.resetPwd(email,newPwd);
        return Result.success();
    }
}
