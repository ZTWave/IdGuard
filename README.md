# IdGuard

[![](https://jitpack.io/v/ZTWave/IdGurad.svg)](https://jitpack.io/#ZTWave/IdGurad)

#### 介绍
Android 资源混淆

#### 使用说明

 **注：请在执行操作前备份代码** 

在`project`的`build.gradle`中的`buildscript`下添加

```groovy
repositories {  
  maven { url 'https://jitpack.io' }  
}  
dependencies {  
  classpath 'com.github.ZTWave:IdGuard:0.3.4'  
}
```

在app下的build.gradle中添加

```groovy
plugins {
  id 'idguard'
}
```

如果你有白名单需求请在app module 下添加。支持包名和文件名指定

```groovy
idGuard{
    whiteList = [
        "com.littlew.example.pc",
        "com.littlew.example.pa",
        //support single java file
        "com.littlew.example.pf.A.java",
    ]
}
```

在`Android Studio` 的`Task`中可以找到 `LayoutGuard` `IdGuard` `ResGuard` 这三个`Task `

1. `LayoutGuard` : 可以将`layout`文件进行随机命名并更新引用
2. `IdGuard` : 可以将`view`的`id`进行重命名并更新引用
3. `ResGuard` : 可以把 `mipmap` `drawable` `string` 中的 资源文件进行随机命名并更新引用
4. `ClassGuard` : 可以把`java`文件夹中的所有`java`文件和对应得 类 函数 变量 形参 进行重命名并更新引用

每个 `task` 执行过后会在根目录下输出`mapping`文件，记得及时备份，以防日后查找需要

`0.3.3` 版本更新后，所有已经全部支持多个module情况

![GradleTask](https://pic.imgdb.cn/item/64b62c311ddac507ccff507b.jpg)

#### 注
目前`classGuard` 会对arr或者jar调用的同名方法进行替换，请在`task`完成后进行错误检查!!

#### 感谢

[XmlClassGuard](https://github.com/liujingxing/XmlClassGuard),[qbox](https://github.com/paul-hammant/qdox)
