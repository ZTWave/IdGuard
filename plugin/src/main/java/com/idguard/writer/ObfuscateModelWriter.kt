package com.idguard.writer

import com.idguard.modal.ClazzInfo
import com.idguard.modal.ConstructorInfo
import com.thoughtworks.qdox.model.*
import com.thoughtworks.qdox.model.JavaModuleDescriptor.*
import com.thoughtworks.qdox.model.expression.AnnotationValue
import com.thoughtworks.qdox.model.expression.Expression
import com.thoughtworks.qdox.writer.impl.IndentBuffer

/**
 * clazzinfos is project's
 *
 * use [writeSource] to write source in
 * use [toString] to get obfuscate source out
 */
class ObfuscateModelWriter {
    val buffer = IndentBuffer()

    /**
     * 项目中的所有info 用于替换
     */
    var projectClazzInfos: List<ClazzInfo> = emptyList()

    /**
     * 是否在白名单内
     */
    var inWhiteList = false

    fun writeSource(source: JavaSource) {
        debug("do write source -> $source")
        // package statement
        writePackage(source.getPackage())

        // import statement
        val obfuscateImports = ObfuscateInfoMaker.imports(source.imports, projectClazzInfos)
        debug("obfuscateImports -> $obfuscateImports")
        for (imprt in obfuscateImports) {
            buffer.write("import ")
            buffer.write(imprt)
            buffer.write(';')
            buffer.newline()
        }
        if (source.imports.size > 0) {
            buffer.newline()
        }

        // classes
        val iter: ListIterator<JavaClass> = source.classes.listIterator()
        while (iter.hasNext()) {
            val cls = iter.next()
            writeClass(cls)
            if (iter.hasNext()) {
                buffer.newline()
            }
        }
    }

    private fun writePackage(pckg: JavaPackage?) {
        debug("writePackage -> $pckg")
        if (pckg != null) {
            commentHeader(pckg)
            buffer.write("package ")
            buffer.write(pckg.name)
            buffer.write(';')
            buffer.newline()
            buffer.newline()
        }
    }

    /**
     * temporary not consider class is annotation
     */
    private fun writeClass(cls: JavaClass) {
        debug("do write class -> ${cls.fullyQualifiedName}")

        commentHeader(cls)
        writeAccessibilityModifier(cls.modifiers)
        writeNonAccessibilityModifiers(cls.modifiers)
        buffer.write(
            if (cls.isEnum) {
                "enum "
            } else if (cls.isInterface) {
                "interface "
            } else {
                "class "
            }
        )
        //buffer.write(cls.name)
        buffer.write(ObfuscateInfoMaker.className(cls, projectClazzInfos))

        // superclass
        if (cls.superClass != null) {
            val className = cls.superClass.fullyQualifiedName
            if ("java.lang.Object" != className && "java.lang.Enum" != className) {
                buffer.write(" extends ")
                //buffer.write(cls.superClass.genericCanonicalName)
                val extendStr = ObfuscateInfoMaker.superClassName(cls.superClass, projectClazzInfos)
                buffer.write(extendStr)
            }
        }

        // implements
        if (cls.implements.size > 0) {
            buffer.write(if (cls.isInterface) " extends " else " implements ")
            val iter: ListIterator<JavaType> = cls.implements.listIterator()
            while (iter.hasNext()) {
                //buffer.write(iter.next().genericCanonicalName)
                val implmentStr: String =
                    ObfuscateInfoMaker.implClassName(iter.next(), projectClazzInfos)
                buffer.write(implmentStr)
                if (iter.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        return writeClassBody(cls)
    }

    private fun writeClassBody(cls: JavaClass) {
        val corrObfuscateClassInfo = projectClazzInfos.find { it.isCorrespondingJavaClass(cls) }
            ?: throw RuntimeException("can't find raw class ${cls.fullyQualifiedName} in given class infos")

        buffer.write(" {")
        buffer.newline()
        buffer.indent()

        // fields
        val obfuscateField =
            ObfuscateInfoMaker.field(cls.fields, corrObfuscateClassInfo.fieldList)
        for (javaField in obfuscateField) {
            buffer.newline()
            writeField(javaField)
        }

        // constructors
        val obfuscateConstructor =
            ObfuscateInfoMaker.constructors(
                cls.constructors,
                corrObfuscateClassInfo,
                projectClazzInfos
            )
        for (javaConstructor in obfuscateConstructor) {
            buffer.newline()
            writeConstructor(javaConstructor, corrObfuscateClassInfo.constructors)
        }

        // methods replace
        val obfuscateMethod =
            ObfuscateInfoMaker.methods(
                cls.methods,
                corrObfuscateClassInfo,
                projectClazzInfos
            )
        for (javaMethod in obfuscateMethod) {
            buffer.newline()
            writeMethod(javaMethod, corrObfuscateClassInfo)
        }

        // inner-classes
        // doesn't matter
        for (innerCls in cls.nestedClasses) {
            buffer.newline()
            writeClass(innerCls)
        }
        buffer.deindent()
        buffer.newline()
        buffer.write('}')
        buffer.newline()
    }

    private fun writeInitializer(init: JavaInitializer) {
        if (init.isStatic) {
            buffer.write("static ")
        }
        buffer.write('{')
        buffer.newline()
        buffer.indent()
        buffer.write(init.blockContent)
        buffer.deindent()
        buffer.newline()
        buffer.write('}')
        buffer.newline()

    }

    private fun writeField(field: JavaField) {
        commentHeader(field)
        //not enum field
        if (!field.isEnumConstant) {
            writeAllModifiers(field.modifiers)
            val fieldStr: String = ObfuscateInfoMaker.fieldClazzName(field, projectClazzInfos)
            buffer.write(fieldStr)
//            buffer.write(field.type.genericCanonicalName)
            buffer.write(' ')
        }
        buffer.write(field.name)
        if (field.isEnumConstant) {
            if (field.enumConstantArguments != null && field.enumConstantArguments.isNotEmpty()) {
                buffer.write("( ")
                val iter: Iterator<Expression> = field.enumConstantArguments.listIterator()
                while (iter.hasNext()) {
                    buffer.write(iter.next().parameterValue.toString())
                    if (iter.hasNext()) {
                        buffer.write(", ")
                    }
                }
                buffer.write(" )")
            }
            if (field.enumConstantClass != null) {
                writeClassBody(field.enumConstantClass)
            }
            buffer.write(',')
        } else {
            if (field.initializationExpression != null && field.initializationExpression.isNotEmpty()) {
                run { buffer.write(" = ") }
                buffer.write(field.initializationExpression)
            }
            buffer.write(';')
        }
        buffer.newline()

    }

    /**
     * @param constructors this clazz info all constructors
     */
    private fun writeConstructor(
        constructor: JavaConstructor,
        constructors: List<ConstructorInfo>
    ) {
        commentHeader(constructor)
        writeAllModifiers(constructor.modifiers)
        buffer.write(constructor.name)
        buffer.write('(')

        debug("constructor -> ${constructor.name}")
        debug("params -> ${constructor.parameters}")
        val obResult = ObfuscateInfoMaker.parametersName(constructor, constructors)

        debug("constructor ob params -> ${obResult.first}")
        debug("constructor need replaced -> ${obResult.second}")

        val obParameter = obResult.first
        val iter: ListIterator<JavaParameter> = obParameter.listIterator()
        while (iter.hasNext()) {
            writeParameter(iter.next())
            if (iter.hasNext()) {
                buffer.write(", ")
            }
        }
        buffer.write(')')
        if (constructor.exceptions.size > 0) {
            buffer.write(" throws ")
            val excIter: Iterator<JavaClass> = constructor.exceptions.iterator()
            while (excIter.hasNext()) {
                val newName =
                    ObfuscateInfoMaker.exceptionGenericCanonicalName(
                        excIter.next(),
                        projectClazzInfos
                    )
                buffer.write(newName)
//                buffer.write(excIter.next().genericCanonicalName)
                if (excIter.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        buffer.write(" {")
        //buffer.newline()
        if (constructor.sourceCode != null) {
            val obSourceCode =
                ObfuscateInfoMaker.sourceCode(constructor.sourceCode, obResult.second)
            buffer.write(obSourceCode)
//            buffer.write(constructor.sourceCode)
        }
        buffer.write('}')
        buffer.newline()
    }

    private fun writeMethod(method: JavaMethod, corrObfuscateClassInfo: ClazzInfo) {
        commentHeader(method)
        writeAccessibilityModifier(method.modifiers)
        writeNonAccessibilityModifiers(method.modifiers)

        //buffer.write(method.returnType.genericCanonicalName)
        val returnTypeGenericCanonicalName =
            ObfuscateInfoMaker.returnTypeName(method.returnType, projectClazzInfos)
        buffer.write(returnTypeGenericCanonicalName)

        buffer.write(' ')
        buffer.write(method.name)
        buffer.write('(')

        val obParamsResult =
            ObfuscateInfoMaker.parametersName(method, corrObfuscateClassInfo.methodList)
        val obParams = obParamsResult.first
        val iter: ListIterator<JavaParameter> = obParams.listIterator()
        while (iter.hasNext()) {
            writeParameter(iter.next())
            if (iter.hasNext()) {
                buffer.write(", ")
            }
        }
        buffer.write(')')
        if (method.exceptions.size > 0) {
            buffer.write(" throws ")
            val excIter: Iterator<JavaClass> = method.exceptions.iterator()
            while (excIter.hasNext()) {
                val newName =
                    ObfuscateInfoMaker.exceptionGenericCanonicalName(
                        excIter.next(),
                        projectClazzInfos
                    )
                buffer.write(newName)
                if (excIter.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        if (method.sourceCode != null && method.sourceCode.isNotEmpty()) {
            buffer.write(" {")
            //buffer.newline()
            debug("replace source code -> ${method.sourceCode}")
            val obSourceCode =
                ObfuscateInfoMaker.sourceCode(method.sourceCode, obParamsResult.second)
            buffer.write(obSourceCode)

            buffer.write('}')
            buffer.newline()
        } else {
            buffer.write(';')
            buffer.newline()
        }
    }

    private fun writeNonAccessibilityModifiers(modifiers: Collection<String>) {
        for (modifier in modifiers) {
            // interface is included as a modifier
            // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/reflect/Modifier.html#toString(int)
            if (!modifier.startsWith("p") && "interface" != modifier) {
                buffer.write(modifier)
                buffer.write(' ')
            }
        }
    }

    private fun writeAccessibilityModifier(modifiers: Collection<String>) {
        for (modifier in modifiers) {
            if (modifier.startsWith("p")) {
                buffer.write(modifier)
                buffer.write(' ')
            }
        }
    }

    private fun writeAllModifiers(modifiers: List<String>) {
        for (modifier in modifiers) {
            buffer.write(modifier)
            buffer.write(' ')
        }
    }

    private fun writeAnnotation(annotation: JavaAnnotation) {
        buffer.write('@')
        buffer.write(annotation.type.genericCanonicalName)
        if (annotation.propertyMap.isNotEmpty()) {
            buffer.indent()
            buffer.write('(')
            val iterator: Iterator<Map.Entry<String, AnnotationValue>> =
                annotation.propertyMap.entries.iterator()
            while (iterator.hasNext()) {
                val (key, value) = iterator.next()
                buffer.write(key)
                buffer.write('=')
                buffer.write(value.toString())
                if (iterator.hasNext()) {
                    buffer.write(',')
                    buffer.newline()
                }
            }
            buffer.write(')')
            buffer.deindent()
        }
        buffer.newline()
    }

    private fun writeParameter(parameter: JavaParameter) {
        commentHeader(parameter)
        //buffer.write(parameter.genericCanonicalName)
        val newClassTypeName: String =
            ObfuscateInfoMaker.parameterTypeName(parameter, projectClazzInfos)
        buffer.write(newClassTypeName)
        if (parameter.isVarArgs) {
            buffer.write("...")
        }
        buffer.write(' ')
        buffer.write(parameter.name)
    }

    private fun commentHeader(entity: JavaAnnotatedElement) {
        if (entity.comment != null || entity.tags.size > 0) {
            buffer.write("/**")
            buffer.newline()
            if (entity.comment != null && entity.comment.isNotEmpty()) {
                buffer.write(" * ")
                buffer.write(entity.comment.replace("\n".toRegex(), "\n * "))
                buffer.newline()
            }
            if (entity.tags.size > 0) {
                if (entity.comment != null && entity.comment.isNotEmpty()) {
                    buffer.write(" *")
                    buffer.newline()
                }
                for (docletTag in entity.tags) {
                    buffer.write(" * @")
                    buffer.write(docletTag.name)
                    if (docletTag.value.isNotEmpty()) {
                        buffer.write(' ')
                        buffer.write(docletTag.value)
                    }
                    buffer.newline()
                }
            }
            buffer.write(" */")
            buffer.newline()
        }
        if (entity.annotations != null) {
            for (annotation in entity.annotations) {
                writeAnnotation(annotation)
            }
        }
    }

    private fun writeModuleDescriptor(descriptor: JavaModuleDescriptor) {
        if (descriptor.isOpen) {
            buffer.write("open ")
        }
        buffer.write("module " + descriptor.name + " {")
        buffer.newline()
        buffer.indent()
        for (requires in descriptor.requires) {
            buffer.newline()
            writeModuleRequires(requires)
        }
        for (exports in descriptor.exports) {
            buffer.newline()
            writeModuleExports(exports)
        }
        for (opens in descriptor.opens) {
            buffer.newline()
            writeModuleOpens(opens)
        }
        for (provides in descriptor.provides) {
            buffer.newline()
            writeModuleProvides(provides)
        }
        for (uses in descriptor.uses) {
            buffer.newline()
            writeModuleUses(uses)
        }
        buffer.newline()
        buffer.deindent()
        buffer.write('}')
        buffer.newline()
    }

    private fun writeModuleExports(exports: JavaExports) {
        buffer.write("exports ")
        buffer.write(exports.source.name)
        if (!exports.targets.isEmpty()) {
            buffer.write(" to ")
            val targets: Iterator<JavaModule> = exports.targets.iterator()
            while (targets.hasNext()) {
                val target = targets.next()
                buffer.write(target.name)
                if (targets.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        buffer.write(';')
        buffer.newline()
    }

    private fun writeModuleOpens(opens: JavaOpens) {
        buffer.write("opens ")
        buffer.write(opens.source.name)
        if (!opens.targets.isEmpty()) {
            buffer.write(" to ")
            val targets: Iterator<JavaModule> = opens.targets.iterator()
            while (targets.hasNext()) {
                val target = targets.next()
                buffer.write(target.name)
                if (targets.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        buffer.write(';')
        buffer.newline()
    }

    private fun writeModuleProvides(provides: JavaProvides) {
        buffer.write("provides ")
        buffer.write(provides.service.name)
        buffer.write(" with ")
        val providers: Iterator<JavaClass> = provides.providers.iterator()
        while (providers.hasNext()) {
            val provider = providers.next()
            buffer.write(provider.name)
            if (providers.hasNext()) {
                buffer.write(", ")
            }
        }
        buffer.write(';')
        buffer.newline()
    }

    private fun writeModuleRequires(requires: JavaRequires) {
        buffer.write("requires ")
        writeAccessibilityModifier(requires.modifiers)
        writeNonAccessibilityModifiers(requires.modifiers)
        buffer.write(requires.module.name)
        buffer.write(';')
        buffer.newline()
    }

    private fun writeModuleUses(uses: JavaUses) {
        buffer.write("uses ")
        buffer.write(uses.service.name)
        buffer.write(';')
        buffer.newline()
    }

    override fun toString(): String = buffer.toString()

    private fun debug(msg: String) {
        println("model writer -> $msg")
    }
}