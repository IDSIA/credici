package ch.idsia.model;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.utility.ArraysUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.Assert.*;

public class StructuralCausalModelTest {

    private static StructuralCausalModel emptyModel(){
        return new StructuralCausalModel("emptyModel");
    }
    private static StructuralCausalModel modelSimple(){
        StructuralCausalModel m = new StructuralCausalModel("modelSimple");
        int x = m.addVariable(2);
        int u = m.addVariable(3, true);
        m.addParent(x,u);
        m.fillWithRandomFactors(2);
        return m;
    }
    private static StructuralCausalModel modelSimple2(){
        StructuralCausalModel m = new StructuralCausalModel("modelSimple2");
        int x = m.addVariable(2);
        int y = m.addVariable(4);
        int ux = m.addVariable(3, true);
        int uy = m.addVariable(5, true);
        m.addParent(x,ux);
        m.addParent(y,uy);
        m.addParent(y,x);
        m.fillWithRandomFactors(2);
        return m;
    }
    private static StructuralCausalModel modelSingleU(){
        StructuralCausalModel m = new StructuralCausalModel("modelSingleU");
        int x = m.addVariable(2);
        int y = m.addVariable(4);
        int u = m.addVariable(8, true);
        m.addParent(x,u);
        m.addParent(y,u);
        m.addParent(y,x);
        m.fillWithRandomFactors(2);
        return m;
    }


    @ParameterizedTest
    @MethodSource("copyParams")
    void copy(StructuralCausalModel model) {

        StructuralCausalModel copy = model.copy();

        for(int v:model.getVariables()){
            assertTrue(ArraysUtil.contains(v, copy.getVariables()));
            assertEquals(copy.isEndogenous(v), model.isEndogenous(v));
            assertArrayEquals(copy.getParents(v), model.getParents(v));
            assertArrayEquals(copy.getFactor(v).getData(), model.getFactor(v).getData(),0);
            assertArrayEquals(copy.getFactor(v).getDomain().getVariables(), model.getFactor(v).getDomain().getVariables());
        }


    }
    private static Stream<Arguments> copyParams() {
        return Stream.of(
                Arguments.of(emptyModel()),
                Arguments.of(modelSimple()),
                Arguments.of(modelSimple2()),
                Arguments.of(modelSingleU())
        );
    }


    @ParameterizedTest
    @MethodSource("addVariableParams")
    void addVariable(StructuralCausalModel model, int expected ) {
        assertEquals(model.addVariable(3, false),expected);
    }

    private static Stream<Arguments> addVariableParams() {
        return Stream.of(
                Arguments.of(emptyModel(),  0),
                Arguments.of(modelSimple(), 2),
                Arguments.of(modelSimple2(), 4),
                Arguments.of(modelSingleU(), 3)
                );
    }
    @ParameterizedTest
    @MethodSource("removeVariableParams")
    void removeVariable(StructuralCausalModel model, int var ) {
        model.removeVariable(var);

        assertFalse(ArraysUtil.contains(var, model.getVariables()));
        assertFalse(ArraysUtil.contains(var, model.getEndogenousVars()));
        assertFalse(ArraysUtil.contains(var, model.getExogenousVars()));


    }

    private static Stream<Arguments> removeVariableParams() {
        return Stream.of(
                Arguments.of(modelSimple(), 0),
                Arguments.of(modelSimple(), 1),

                Arguments.of(modelSimple2(), 0),
                Arguments.of(modelSimple2(), 1),
                Arguments.of(modelSimple2(), 2),

                Arguments.of(modelSingleU(), 0),
                Arguments.of(modelSingleU(), 1),
                Arguments.of(modelSingleU(), 2)

        );
    }

/*
//todo: fix problem in this test

    @ParameterizedTest
    @MethodSource("mergeParams")
    void merge(StructuralCausalModel model,  StructuralCausalModel[] do_models) {

        StructuralCausalModel cfmodel = model.merge(do_models);
        assertTrue(cfmodel.correctFactorDomains());

        for(int world=1; world<=do_models.length; world++){
            StructuralCausalModel m = do_models[world-1];
            for(int x: m.getEndogenousVars()){
               assertEquals(m.getParents(x).length, cfmodel.getParents(cfmodel.getMap().getEquivalentVars(world, x)).length);
               assertArrayEquals(cfmodel.getMap().getEquivalentVars(world, m.getEndegenousParents(x)), cfmodel.getEndegenousParents(cfmodel.getMap().getEquivalentVars(world, x)));
               assertArrayEquals(m.getExogenousParents(x), cfmodel.getExogenousParents(cfmodel.getMap().getEquivalentVars(world, x)));

            }


        }


    }

    private static Stream<Arguments> mergeParams() {
        return Stream.of(
                Arguments.of(modelSimple2(), new StructuralCausalModel[]{
                        modelSimple2().intervention(0,1)
                }),
                Arguments.of(modelSimple2(), new StructuralCausalModel[]{
                        modelSimple2().intervention(1,1)
                }),
                Arguments.of(modelSimple2(), new StructuralCausalModel[]{
                        modelSimple2().intervention(0,1),
                        modelSimple2().intervention(1,1)}),

                Arguments.of(modelSingleU(), new StructuralCausalModel[]{
                        modelSingleU().intervention(0,1)}),
                Arguments.of(modelSingleU(), new StructuralCausalModel[]{
                        modelSingleU().intervention(1,1)}),
                Arguments.of(modelSingleU(), new StructuralCausalModel[]{
                        modelSingleU().intervention(0,1),
                        modelSingleU().intervention(1,1)})

        );
    }

*/



}
