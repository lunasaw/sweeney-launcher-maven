package com.luna.sweeney.launcher;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.google.common.collect.ImmutableMap;
import com.luna.sweeney.support.UserSupport;
import com.luna.sweeney.utils.ProcessUtils;

import oshi.software.os.OSProcess;

/**
 * @Author: luna
 * @CreateTime: 2020/9/24 12:18
 * @Description:
 */
public class Launcher {

    private Text                password;
    private Text                mark;

    protected Shell             shell;

    private static final String USER_SETTING_JSON = "UserSetting.json";

    private static final String JAVA              = "java";
    private static final String SWEENEY_LAUNCHER  = "sweeney-launcher";

    private static final String JRE               = "jre/bin/java";
    private static final String LOG               = "log/console.log." + System.currentTimeMillis();

    private static final String JAR_REGEX         = "sweeney-server-.*-SNAPSHOT\\.jar";

    /**
     * Launch the application.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (isMultiInstance()) {
                return;
            }

            killAll();

            if (!checkLoginToken()) {
                new Launcher().open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Open the window.
     */
    public void open() {
        final Display display = new Display();
        createContent();
        shell.open();
        shell.layout();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.isDisposed();
    }

    private static boolean checkLoginToken() {
        File tokenFile = new File(USER_SETTING_JSON);

        if (!tokenFile.exists()) {
            return false;
        }

        try {
            String s = FileUtils.readFileToString(tokenFile, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSON.parseObject(s);

            String sessionKey = null;
            try {
                sessionKey = UserSupport.login(jsonObject.getString("userMark"), jsonObject.getString("password"));
            } catch (RuntimeException ignore) {
            }

            if (StringUtils.isBlank(sessionKey)) {
                tokenFile.deleteOnExit();
                return false;
            }

            startSweeney();
            return true;
        } catch (IOException ie) {
            ie.printStackTrace();
            return false;
        }
    }

    private void writeUserSetting(String json) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(new File(USER_SETTING_JSON), json,
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findLatestJar() {
        double maxVersion;
        try {
            maxVersion = Files.walk(Paths.get(""), 1).filter(Files::isRegularFile)
                .filter(path -> Pattern.matches(JAR_REGEX, path.getFileName().toString()))
                .map(path -> Double.parseDouble(path.getFileName().toString().split("-")[2]))
                .max(Comparator.comparing(Double::valueOf)).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "sweeney-server-" + maxVersion + "-SNAPSHOT.jar";
    }

    private static boolean isMultiInstance() {
        return ProcessUtils.getProcessesByPath(new File("").getAbsolutePath()).stream().filter(
            osProcess -> osProcess.getPath().contains(JAVA) && osProcess.getCommandLine().contains(SWEENEY_LAUNCHER))
            .count() > 1;
    }

    private static void killAll() {
        int selfProcessId = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);

        List<OSProcess> osProcessList = ProcessUtils.getProcessesByPath(new File("").getAbsolutePath());

        for (OSProcess osProcess : osProcessList) {
            // 杀掉除自己以外的进程
            if (osProcess.getProcessID() != selfProcessId) {
                ProcessUtils.osKill(osProcess.getProcessID());
            }
        }
    }

    private static void startSweeney() {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(JRE, "-jar", findLatestJar());
            Process process = builder.start();
            InputStream in = process.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(LOG);
            IOUtils.copy(in, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create contents of the window.
     * @wbp.parser.entryPoint
     */
    private void createContent() {
        shell = new Shell(SWT.CLOSE | SWT.MIN);
        shell.setSize(470, 190);
        shell.setText("Sweeney");

        mark = new Text(shell, SWT.BORDER);
        mark.setBounds(143, 30, 175, 23);

        password = new Text(shell, SWT.BORDER | SWT.PASSWORD);
        password.setBounds(143, 67, 175, 23);

        Button login = new Button(shell, SWT.ABORT);
        login.setText("登录");
        login.setBounds(174, 107, 115, 27);
        login.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (StringUtils.isEmpty(mark.getText()) || StringUtils.isEmpty(password.getText())) {
                    MessageDialog.openError(shell, "错误", "用户名或者密码为空，请重新输入");
                    return;
                }

                try {
                    String sessionKey = UserSupport.login(mark.getText(), password.getText());
                    if (StringUtils.isNotBlank(sessionKey)) {
                        writeUserSetting(JSON.toJSONString(ImmutableMap.of(
                            "userMark", mark.getText(),
                            "password", password.getText(),
                            "site", UserSupport.SITE)));

                        shell.dispose();
                        startSweeney();
                    }
                } catch (Throwable t) {
                    MessageDialog.openError(shell, "错误", t.toString());
                }
            }
        });

        Link link = new Link(shell, SWT.NONE);
        link.setBounds(344, 33, 65, 17);
        link.setText("<a>注册账号</a>");
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (java.awt.Desktop.isDesktopSupported()) {
                    URI uri = URI.create(
                            "https://wed.iteknical.com/fusion-user/register?site=wednesday&redirectURL=https%3A%2F%2Fwed"
                                    + ".luna.com%2Fwednesday%2Fmy%2Findex");
                    // 判断系统桌面是否支持要执行的功能
                    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        // 获取系统默认浏览器打开链接
                        try {
                            Desktop.getDesktop().browse(uri);
                        } catch (IOException ie) {
                            ie.printStackTrace();
                        }
                    }
                }
            }
        });

        Label label1 = new Label(shell, SWT.NONE);
        label1.setAlignment(SWT.RIGHT);
        label1.setBounds(40, 33, 85, 17);
        label1.setText("邮箱或手机");

        Label label2 = new Label(shell, SWT.NONE);
        label2.setAlignment(SWT.RIGHT);
        label2.setBounds(60, 70, 65, 17);
        label2.setText("密码");
    }
}