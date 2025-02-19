## Eclipse JDT

## Jetbrains PSI

- Probably need to isolate code from idea
  env (https://intellij-support.jetbrains.com/hc/en-us/community/posts/203374950-Use-Grammar-Kit-parser-standalone)
- Not enough AST builders

+ Optimizations on top of generated code (imports, indents, ...)

## Spoon

+ Lightweight
+ Autoimport

? Multiple factories

## JavaParser

+ Lightweight (without symbol parser)
+ Generates raw source code (no project structure required)
+ Object AST representation (no ast builder instance)

- No auto-import or so
- Java specific

[//]: # ()

[//]: # ()

[//]: # (//import org.eclipse.jdt.core.dom.AST)

[//]: # (//import org.eclipse.jdt.core.dom.Modifier)

[//]: # (//import org.eclipse.jdt.core.dom.PrimitiveType)

[//]: # (//import org.eclipse.jdt.internal.core.dom.rewrite.ASTRewriteFormatter)

[//]: # (//import org.eclipse.jface.text.Document)

[//]: # (//implementation&#40;"org.eclipse.jdt:org.eclipse.jdt.core:3.40.0"&#41;)

[//]: # (//fun eclipse&#40;&#41; {)

[//]: # (//    val ast = AST.newAST&#40;AST.getJLSLatest&#40;&#41;, false&#41;)

[//]: # (//    val compUnit = ast.newCompilationUnit&#40;&#41;)

[//]: # (//    compUnit.`package` = ast.newPackageDeclaration&#40;&#41;.apply { name = ast.newName&#40;"a.b.c.d"&#41; })

[//]: # (//    val td = ast.newTypeDeclaration&#40;&#41;)

[//]: # (//    td.name = ast.newSimpleName&#40;"TypeDecl"&#41;)

[//]: # (//    compUnit.types&#40;&#41;.add&#40;td&#41;)

[//]: # (//    val md = ast.newMethodDeclaration&#40;&#41;)

[//]: # (//    md.name = ast.newSimpleName&#40;"method"&#41;)

[//]: # (//    md.modifiers&#40;&#41;.add&#40;ast.newModifier&#40;Modifier.ModifierKeyword.PUBLIC_KEYWORD&#41;&#41;)

[//]: # (//    md.returnType2 = ast.newArrayType&#40;ast.newPrimitiveType&#40;PrimitiveType.DOUBLE&#41;&#41;)

[//]: # (//    val prm = ast.newSingleVariableDeclaration&#40;&#41;)

[//]: # (//    prm.name = ast.newSimpleName&#40;"dabul"&#41;)

[//]: # (//    prm.type = ast.newPrimitiveType&#40;PrimitiveType.DOUBLE&#41;)

[//]: # (//    md.parameters&#40;&#41;.add&#40;prm&#41;)

[//]: # (// double[] kek = new double[] {1.0, 2.0};)

[//]: # (//    val body = ast.newBlock&#40;&#41;)

[//]: # (//    val mkArr = ast.newArrayCreation&#40;&#41;)

[//]: # (//    mkArr.initializer = ast.newArrayInitializer&#40;&#41;.apply {)

[//]: # (//        expressions&#40;&#41;.addAll&#40;)

[//]: # (//            listOf&#40;)

[//]: # (//                ast.newNumberLiteral&#40;"1.0"&#41;,)

[//]: # (//                ast.newNumberLiteral&#40;"2.0"&#41;,)

[//]: # (//                ast.newNumberLiteral&#40;"3.0"&#41;)

[//]: # (//            &#41;)

[//]: # (//        &#41;)

[//]: # (//    })

[//]: # (//    val arrVarFrag = ast.newVariableDeclarationFragment&#40;&#41;)

[//]: # (//    arrVarFrag.name = ast.newSimpleName&#40;"tempVar"&#41;)

[//]: # (//    arrVarFrag.initializer = mkArr)

[//]: # (//    body.statements&#40;&#41;.add&#40;ast.newVariableDeclarationStatement&#40;arrVarFrag&#41;&#41;)

[//]: # (//    md.body = body)

[//]: # (//    td.bodyDeclarations&#40;&#41;.add&#40;md&#41;)

[//]: # (//    println&#40;compUnit.toString&#40;&#41;&#41;)

[//]: # (//})

[//]: # ()

[//]: # (fun spoonGen&#40;&#41; {)

[//]: # (//    val launcher = Launcher&#40;&#41;)

[//]: # (//    launcher.environment.isAutoImports)

[//]: # (//    val baseFact = launcher.modelBuilder.factory)

[//]: # (//    val packageFactory = PackageFactory&#40;baseFact&#41;)

[//]: # (//)

[//]: # (//    val bodyFact = baseFact)

[//]: # (//    val sample: CtType<Any> = baseFact.Class&#40;&#41;.create&#40;"idk.suka.sample"&#41;)

[//]: # (//    val sampleMethod: CtMethod<Any> = baseFact.Method&#40;&#41;.create<Any>&#40;)

[//]: # (//        sample,)

[//]: # (//        setOf&#40;ModifierKind.PUBLIC&#41;,)

[//]: # (//        baseFact.createCtTypeReference<Any>&#40;Unit.javaClass&#41;,)

[//]: # (//        "keker",)

[//]: # (//        mutableListOf<Parameter>&#40;&#41;)

[//]: # (//    &#41;)

[//]: # (//    println&#40;baseFact.Package&#40;&#41;.create&#40;null, "idk.blya"&#41;.prettyprint&#40;&#41;&#41;)

[//]: # (//    println&#40;sample.prettyprint&#40;&#41;&#41;)

[//]: # (})

## Import strategy

Before rendering any AST elements, collect all the types from UTest, counting entries of each and check if it is
possible to use simple name.

It may be possible to choose the name on-the-fly using, for example `data class Ref<T>(var v: T)`

### Import problem 

When parsing existing file, fully qualified name may be parsed as a field access. Possible solution is to use a visitor
traversing ast that checks if some field.toString is in the classpath of project. 

It seems there is no way resolving this without extra info about classpath


## Var declaration strategy

AllocCall, ObjectCreation and CastExpr introduce instances. Count all such entries and if it is used more than once - we
need the var before the line it was first used.


by controller methods for paths 