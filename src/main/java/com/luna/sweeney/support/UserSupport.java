package com.luna.sweeney.support;

import com.luna.common.dto.ResultDTOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import com.luna.common.dto.ResultDTO;
import com.luna.fusion.user.client.UserClient;
import com.luna.fusion.user.vo.LoginVO;

/**
 * @author Tony
 */
public class UserSupport {
    private static final Logger     logger      = LoggerFactory.getLogger(UserSupport.class);

    public static final String SITE = "sweeney";
    private static final String FUSION_USER = "http://localhost:8001";

    private static final UserClient USER_CLIENT = new UserClient(FUSION_USER);

    public static String login(String userMark, String password) {
        LoginVO loginVO = new LoginVO();
        loginVO.setUserMark(userMark);
        loginVO.setPassword(password);
        return login(loginVO);
    }

    public static String login(LoginVO loginVO) {
        loginVO.setSite(SITE);
        ResultDTO<String> resultDTO = USER_CLIENT.login(loginVO);
        logger.info("userClient.login, loginVO={}, site={}, resultDTO={}", loginVO, SITE, JSON.toJSONString(resultDTO));
        return ResultDTOUtils.checkResultAndGetData(resultDTO);
    }
}
