package rendering;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Provider;
import com.github.javaparser.StringProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class ParserWorkaround {
    public static void main(String[] args) {
        // Configure type solvers
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver()); // Resolves JRE types

        // Configure JavaParser with symbol resolution
        ParserConfiguration parserConfig = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        JavaParser javaParser = new JavaParser(parserConfig);

        // Parse code with fully qualified names
        String code = "public class S1 { public void kek() { java.util.List<String> list = new java.util.ArrayList<>(); } } ";
        CompilationUnit cu = javaParser.parse(code).getResult().get();
        System.out.println(cu.toString());
    }
}
