package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisComponent;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.entity.vo.UserInfoVO;
import com.easypan.service.FileInfoService;
import com.easypan.service.UserInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("adminController")
@RequestMapping("/admin")
@Api(tags = "管理员")
public class AdminController extends CommonFileController {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private FileInfoService fileInfoService;

    @ApiOperation("获取系统设置")
    @RequestMapping("/getSysSettings")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO getSysSettings() {
        return getSuccessResponseVO(redisComponent.getSysSettingsDto());
    }


    @ApiOperation("保存系统设置")
    @RequestMapping("/saveSysSettings")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO saveSysSettings(
            @VerifyParam(required = true) String registerEmailTitle,
            @VerifyParam(required = true) String registerEmailContent,
            @VerifyParam(required = true) Integer userInitUseSpace) {
        SysSettingsDto sysSettingsDto = new SysSettingsDto();
        sysSettingsDto.setRegisterEmailTitle(registerEmailTitle);
        sysSettingsDto.setRegisterEmailContent(registerEmailContent);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        redisComponent.saveSysSettingsDto(sysSettingsDto);
        return getSuccessResponseVO(null);
    }

    @ApiOperation("加载用户列表")
    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, UserInfoVO.class));
    }


    @ApiOperation("更新用户状态")
    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO updateUserStatus(@VerifyParam(required = true) String userId, @VerifyParam(required = true) Integer status) {
        userInfoService.updateUserStatus(userId, status);
        return getSuccessResponseVO(null);
    }

    @ApiOperation("更新用户空间")
    @RequestMapping("/updateUserSpace")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO updateUserSpace(@VerifyParam(required = true) String userId, @VerifyParam(required = true) Integer changeSpace) {
        userInfoService.changeUserSpace(userId, changeSpace);
        return getSuccessResponseVO(null);
    }

    /**
     * 查询所有文件
     *
     * @param query
     * @return
     */
    @ApiOperation("加载所有文件列表")
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadDataList(FileInfoQuery query) {
        query.setOrderBy("last_update_time desc");
        query.setQueryNickName(true);
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(resultVO);
    }

    @ApiOperation("获取文件夹路径（管理员）")
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkLogin = false,checkAdmin = true, checkParams = true)
    public ResponseVO getFolderInfo(@VerifyParam(required = true) String path) {
        return super.getFolderInfo(path, null);
    }


    @ApiOperation("获取用户文件（管理员）")
    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public void getFile(HttpServletResponse response,
                        @PathVariable("userId") @VerifyParam(required = true) String userId,
                        @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        super.getFile(response, fileId, userId);
    }


    @ApiOperation("获取用户视频分片（管理员）")
    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("userId") @VerifyParam(required = true) String userId,
                             @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        super.getFile(response, fileId, userId);
    }

    @ApiOperation("生成下载链接（管理员）")
    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO createDownloadUrl(@PathVariable("userId") @VerifyParam(required = true) String userId,
                                        @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        return super.createDownloadUrl(fileId, userId);
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @ApiOperation("下载文件（管理员）")
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }


    @ApiOperation("删除文件（管理员）")
    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO delFile(@VerifyParam(required = true) String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0], itemArray[1], true);
        }
        return getSuccessResponseVO(null);
    }
}
