package com.chao.servlet;

import com.chao.annoation.*;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by LuZichao on  2018/12/17 11:52
 */
public class DispatcherServlet extends HttpServlet {

    private List<String> classNames = new ArrayList<String>();

    private Map<String, Object> beans = new ConcurrentHashMap<>();

    private Map<String, Object> handlerMap = new HashMap<>();

    //这里声明一个controller bean的map 是为了反射调用方法的时候能够获取实例，key 与方法的url相同
    private Map<String, Object> controllerHandlerMap = new HashMap<>();

    public void init() throws ServletException {
        //把所有的bean扫描出来
        scanPackage("com.chao");

        //根据全类名 进行实例化
        doInstance();

        //进行注入
        doDI();

        bulidUrlMapping();
    }

    private void scanPackage(String packages) {
        String urlPath = "/" + packages.replaceAll("\\.", "/");
        URL url = this.getClass().getClassLoader().getResource(urlPath.replace("classes","target"));
         File file = new File(url.getFile());
        String[] files = file.list();
        for (String path : files) {
            File scanFile = new File(url.getFile() + path);
            if (scanFile.isDirectory()) {
                scanPackage(packages+"."+path);
            } else {
                classNames.add(packages + "." + scanFile.getName());
            }
        }
    }

    //进行bean的实例化
    private void doInstance() {
        if (classNames.size() <= 0) {
            System.out.println("包扫描失败");
            return;
        }

        classNames.forEach(className -> {
            try {
                Class<?> clazz = Class.forName(className.replace(".class", ""));

                if (clazz.isAnnotationPresent(ChaoController.class)) {
                    Object instance = clazz.newInstance(); //创建控制类
                    ChaoRequestMapping chaoRequestMapping = clazz.getAnnotation(ChaoRequestMapping.class);
                    String value = chaoRequestMapping.value(); //这里可以进行判断 如果为空则用类名首字母小写，这里没判断只写思路
                    beans.put(value, instance); //放到ioc容器
                } else if (clazz.isAnnotationPresent(ChaoService.class)) {
                    Object instance = clazz.newInstance(); //创建控制类
                    ChaoService chaoService = clazz.getAnnotation(ChaoService.class);
                    beans.put(chaoService.value(), instance); //放到ioc容器
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //把service注入到控制层
    private void doDI() {
        if (beans.entrySet().size() <= 0) {
            System.out.println("无实例化的类");
        }

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();

            if (clazz.isAnnotationPresent(ChaoController.class)) {
                Field[] fields = clazz.getDeclaredFields(); //getDeclaredFields私有属性也能够获取
                for (Field field : fields) {
                    if (field.isAnnotationPresent(ChaoAutowired.class)) {
                        ChaoAutowired chaoAutowired = field.getAnnotation(ChaoAutowired.class);
                        String value = chaoAutowired.value();
                        field.setAccessible(true); //打开私有属性的权限
                        try {
                            field.set(instance, beans.get(value));//给autowired 的属性赋值
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void bulidUrlMapping() {
        if (beans.entrySet().size() <= 0) {
            System.out.println("无实例化的类");
        }
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(ChaoController.class)) {
                ChaoRequestMapping chaoRequestMapping = clazz.getAnnotation(ChaoRequestMapping.class);
                String classUrl = chaoRequestMapping.value();
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(ChaoRequestMapping.class)) {
                        ChaoRequestMapping methodAnnotation = method.getAnnotation(ChaoRequestMapping.class);
                        String methodUrl = methodAnnotation.value();
                        String url = classUrl + methodUrl;
                        handlerMap.put(url, method); //这里的路径是需要处理的 可能是/main query 少/ 这里暂不进行处理
                        controllerHandlerMap.put(url, instance);
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取请求路径
        String uri = req.getRequestURI(); //  /myspringmvc/main/query

        if (!handlerMap.containsKey(uri)){
            resp.getWriter().write("404 NOT FOUND");
            return;
        }

        //得到项目根路径
        String contextPath = req.getContextPath(); // /myspringmvc

        String urlPath = uri.replace(contextPath, ""); // /main/query

        Method method = (Method) handlerMap.get(urlPath);

        Object[] params = hand(req, resp, method);

        try {
            method.invoke(controllerHandlerMap.get(urlPath), params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Object[] hand(HttpServletRequest request, HttpServletResponse response, Method method) {
        //拿到当前待执行的方法有哪些参数类型
        Class<?>[] parameterTypes = method.getParameterTypes();

        Object[] args = new Object[parameterTypes.length];

        int args_i = 0;
        int index = 0;

        for (Class<?> parameterType : parameterTypes) {
            if (ServletRequest.class.isAssignableFrom(parameterType)) {
                args[args_i++] = request;
            }
            if (ServletRequest.class.isAssignableFrom(parameterType)) {
                args[args_i++] = response;
            }

            Annotation[] paramAns = method.getParameterAnnotations()[index];

            if (paramAns.length > 0) {
                for (Annotation paramAn : paramAns) {
                    if (ChaoRequestParam.class.isAssignableFrom(paramAn.getClass())) {
                        ChaoRequestParam requestParam = (ChaoRequestParam) paramAn;
                        args[args_i++] = request.getParameter(requestParam.value());
                    }
                }
            }
            index++;
        }
        return args;
    }
}
