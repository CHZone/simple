package com.cai.mvc.servlet;

import com.cai.di.annotation.Autowired;
import com.cai.ioc.annotation.Controller;
import com.cai.ioc.annotation.Service;
import com.cai.mvc.annotation.RequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {

    // 为毛要static
    private static final String LOCATION = "contextConfigLocation";

    // 保持所有配置信息
    private Properties properties = new Properties();

    // 保持所有扫描到的类名
    private List<String> classNames = new ArrayList<>();

    //IoC容器
    private Map<String, Object> ioc = new HashMap<>();

    //url->method
    private Map<String, Object> handlerMapping = new HashMap<>();

    public DispatcherServlet(){
        super();
    }

    private void doLoadConfig(String location){
        InputStream is = null;
        try {
            // 相对路径为ClassPath
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            // 读取properties
            properties.load(is);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(null != is){
                    is.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void doScan(String packageName){
        URL url = this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for(File file: dir.listFiles()){
            if(file.isDirectory()){
                doScan(packageName+"."+file.getName());
            }else{
                classNames.add(packageName+"."+file.getName().replace(".class","").trim());
            }
        }
    }

    private String lowerFirstCase(String className){
        char[] chars = className.toCharArray();
        if(chars[0]>='A' && chars[0]<='Z'){
            chars[0] = (char)(chars[0]+32);
        }
        return String.valueOf(chars);
    }

    private void doInstance(){
        if(classNames.size()==0){
            return;
        }

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(Controller.class)){
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,clazz.newInstance());
                }else if(clazz.isAnnotationPresent(Service.class)){
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    if(!"".equals(beanName.trim())){
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    Class<?> [] interfaces = clazz.getInterfaces();
                    for(Class<?> i : interfaces){
                        // 为毛没把首字母改成小写？
                        ioc.put(i.getName(), clazz.newInstance());
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void doAutowired(){
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String, Object> entry: ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if(!field.isAnnotationPresent(Autowired.class)){
                    continue;
                }

                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);

                try{
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    private void initHandlerMapping(){
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry entry: ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();

            if(!clazz.isAnnotationPresent(Controller.class)){
                continue;
            }

            String baseUrl = "";

            if(clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }


            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if(!method.isAnnotationPresent(RequestMapping.class)){
                    continue;
                }

                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String url = ("/"+baseUrl+"/"+requestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
                System.out.println("mapped "+url+","+method);
            }
        }
    }
    /**
     * 初始化：
     * 1. 加载配置文件
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1. 加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        // 2. 扫描相关类
        doScan((String)properties.get("scanPackage"));

        // 3. IoC
        doInstance();

        // 4. DI
        doAutowired();

        // 5. 构造HandlerMapping
        initHandlerMapping();

        System.out.println("framework init over!!!");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            this.doPost(req,resp);
        } catch (Exception e){
            resp.getWriter().write(
                    "500 Exception. Details:\r\n"+ Arrays.toString(e.getStackTrace())
                    .replaceAll("\\[|\\]", "")
                    .replaceAll(", \\s", "\r\n")
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(handlerMapping.isEmpty()){
            return ;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        System.out.println("URI: "+req.getRequestURI());
        System.out.println("URL: "+req.getRequestURL());

        url = url.replaceAll(contextPath, "")
                .replaceAll("/+", "/");

        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!");
            return;
        }

        // 请求参数
        Map<String, String[]> params = req.getParameterMap();
        Method method = (Method)this.handlerMapping.get(url);
        // 参数类型列表
        Class<?>[] paramTypes = method.getParameterTypes();
        // 参数值数组
        Object[] paramValues = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class paramType = paramTypes[i];
            if(paramType == HttpServletRequest.class){
                paramValues[i] = req;
            }else if(paramType == HttpServletResponse.class){
                paramValues[i] = resp;
                // 其他参数只能是string
            }else  if(paramType == String.class)
            {
                for (Map.Entry param : params.entrySet()) {
                    System.out.println("request param: "+param.getKey()+"->"+param.getValue());
                    String value = Arrays.toString((String[])param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s",",");

                    paramValues[i] = value;
                }
            }
        }

        try{
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            // 反射调用
            method.invoke(ioc.get(beanName), paramValues);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
