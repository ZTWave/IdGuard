package com.idguard.writer

import com.idguard.modal.ClazzInfo
import com.thoughtworks.qdox.model.*
import com.thoughtworks.qdox.model.JavaModuleDescriptor.*
import com.thoughtworks.qdox.model.expression.AnnotationValue
import com.thoughtworks.qdox.model.expression.Expression
import com.thoughtworks.qdox.writer.ModelWriter
import com.thoughtworks.qdox.writer.impl.IndentBuffer

/**
 * clazzinfos is project's
 */
class ObfuscateModelWriter(val clazzInfos: List<ClazzInfo>) : ModelWriter {
    val buffer = IndentBuffer()

    override fun writeSource(source: JavaSource): ModelWriter? {
        // package statement
        writePackage(source.getPackage())

        // import statement
        val obfuscateImports = ObfuscateInfoMaker.imports(source.imports, clazzInfos)
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
        return this
    }

    /** {@inheritDoc}  */
    override fun writePackage(pckg: JavaPackage?): ModelWriter? {
        if (pckg != null) {
            commentHeader(pckg)
            buffer.write("package ")
            buffer.write(pckg.name)
            buffer.write(';')
            buffer.newline()
            buffer.newline()
        }
        return this
    }

    /**
     * temporary not consider class is annotation
     */
    override fun writeClass(cls: JavaClass): ModelWriter? {
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
        buffer.write(ObfuscateInfoMaker.className(cls, clazzInfos))

        // subclass
        if (cls.superClass != null) {
            val className = cls.superClass.fullyQualifiedName
            if ("java.lang.Object" != className && "java.lang.Enum" != className) {
                buffer.write(" extends ")
                //buffer.write(cls.superClass.genericCanonicalName)
                buffer.write(ObfuscateInfoMaker.className(cls, clazzInfos))
            }
        }

        // implements
        if (cls.implements.size > 0) {
            buffer.write(if (cls.isInterface) " extends " else " implements ")
            val iter: ListIterator<JavaType> = cls.implements.listIterator()
            while (iter.hasNext()) {
                //buffer.write(iter.next().genericCanonicalName)
                buffer.write(ObfuscateInfoMaker.className(iter.next(), clazzInfos))
                if (iter.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        return writeClassBody(cls)
    }

    private fun writeClassBody(cls: JavaClass): ModelWriter {
        val corrObfuscateClassInfo = clazzInfos.find { it.isCorrespondingJavaClass(cls) }
            ?: throw RuntimeException("can't find raw class ${cls.fullyQualifiedName} in given class infos")

        buffer.write(" {")
        buffer.newline()
        buffer.indent()

        // fields
        val obfuscateField = ObfuscateInfoMaker.field(cls.fields, corrObfuscateClassInfo.fieldList)
        for (javaField in obfuscateField) {
            buffer.newline()
            writeField(javaField)
        }

        // constructors
        val obfuscateConstructor = ObfuscateInfoMaker.constructors(cls.constructors, corrObfuscateClassInfo)
        for (javaConstructor in obfuscateConstructor) {
            buffer.newline()
            writeConstructor(javaConstructor)
        }

        // methods
        val obfuscateMethod = ObfuscateInfoMaker.method(cls.methods, corrObfuscateClassInfo.methodList)
        for (javaMethod in obfuscateMethod) {
            buffer.newline()
            writeMethod(javaMethod)
        }

        // inner-classes
        // TODO: 没有关联 嵌套的子节点
        for (innerCls in cls.nestedClasses) {
            buffer.newline()
            writeClass(innerCls)
        }
        buffer.deindent()
        buffer.newline()
        buffer.write('}')
        buffer.newline()
        return this
    }

    /** {@inheritDoc}  */
    override fun writeInitializer(init: JavaInitializer): ModelWriter? {
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
        return this
    }

    override fun writeField(field: JavaField): ModelWriter {
        commentHeader(field)
        writeAllModifiers(field.modifiers)
        if (!field.isEnumConstant) {
            buffer.write(field.type.genericCanonicalName)
            buffer.write(' ')
        }
        buffer.write(field.name)
        if (field.isEnumConstant) {
            if (field.enumConstantArguments != null && !field.enumConstantArguments.isEmpty()) {
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
        } else {
            if (field.initializationExpression != null && field.initializationExpression.length > 0) {
                run { buffer.write(" = ") }
                buffer.write(field.initializationExpression)
            }
        }
        buffer.write(';')
        buffer.newline()
        return this
    }

    /** {@inheritDoc}  */
    override fun writeConstructor(constructor: JavaConstructor): ModelWriter? {
        commentHeader(constructor)
        writeAllModifiers(constructor.modifiers)
        buffer.write(constructor.name)
        buffer.write('(')
        val iter: ListIterator<JavaParameter> = constructor.parameters.listIterator()
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
                buffer.write(excIter.next().genericCanonicalName)
                if (excIter.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        buffer.write(" {")
        buffer.newline()
        if (constructor.sourceCode != null) {
            buffer.write(constructor.sourceCode)
        }
        buffer.write('}')
        buffer.newline()
        return this
    }

    /** {@inheritDoc}  */
    override fun writeMethod(method: JavaMethod): ModelWriter? {
        commentHeader(method)
        writeAccessibilityModifier(method.modifiers)
        writeNonAccessibilityModifiers(method.modifiers)
        buffer.write(method.returnType.genericCanonicalName)
        buffer.write(' ')
        buffer.write(method.name)
        buffer.write('(')
        val iter: ListIterator<JavaParameter> = method.parameters.listIterator()
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
                buffer.write(excIter.next().genericCanonicalName)
                if (excIter.hasNext()) {
                    buffer.write(", ")
                }
            }
        }
        if (method.sourceCode != null && method.sourceCode.length > 0) {
            buffer.write(" {")
            buffer.newline()
            buffer.write(method.sourceCode)
            buffer.write('}')
            buffer.newline()
        } else {
            buffer.write(';')
            buffer.newline()
        }
        return this
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

    /** {@inheritDoc}  */
    override fun writeAnnotation(annotation: JavaAnnotation): ModelWriter? {
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
        return this
    }

    /** {@inheritDoc}  */
    override fun writeParameter(parameter: JavaParameter): ModelWriter {
        commentHeader(parameter)
        buffer.write(parameter.genericCanonicalName)
        if (parameter.isVarArgs) {
            buffer.write("...")
        }
        buffer.write(' ')
        buffer.write(parameter.name)
        return this
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

    /** {@inheritDoc}  */
    override fun writeModuleDescriptor(descriptor: JavaModuleDescriptor): ModelWriter {
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
        return this
    }

    /** {@inheritDoc}  */
    override fun writeModuleExports(exports: JavaExports): ModelWriter {
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
        return this
    }

    /** {@inheritDoc}  */
    override fun writeModuleOpens(opens: JavaOpens): ModelWriter {
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
        return this
    }

    /** {@inheritDoc}  */
    override fun writeModuleProvides(provides: JavaProvides): ModelWriter? {
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
        return null
    }

    /** {@inheritDoc}  */
    override fun writeModuleRequires(requires: JavaRequires): ModelWriter {
        buffer.write("requires ")
        writeAccessibilityModifier(requires.modifiers)
        writeNonAccessibilityModifiers(requires.modifiers)
        buffer.write(requires.module.name)
        buffer.write(';')
        buffer.newline()
        return this
    }

    /** {@inheritDoc}  */
    override fun writeModuleUses(uses: JavaUses): ModelWriter {
        buffer.write("uses ")
        buffer.write(uses.service.name)
        buffer.write(';')
        buffer.newline()
        return this
    }

    override fun toString(): String = buffer.toString()
}