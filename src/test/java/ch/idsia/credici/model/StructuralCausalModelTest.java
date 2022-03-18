package ch.idsia.credici.model;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.ArraysUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
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

    private static StructuralCausalModel syntheticModel() throws IOException {
        Path wdir = Path.of("");
        Path modelfile = wdir.resolve("./models/synthetic/chain_twExo2_nEndo5_0.uai");
        return (StructuralCausalModel) IO.readUAI(modelfile.toString());
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
                        (StructuralCausalModel) CausalOps.intervention(modelSimple2(),0,1, false)
                }),
                Arguments.of(modelSimple2(), new StructuralCausalModel[]{
                        (StructuralCausalModel) CausalOps.intervention(modelSimple2(),1,1, false)
                }),
                Arguments.of(modelSimple2(), new StructuralCausalModel[]{
                        (StructuralCausalModel) CausalOps.intervention(modelSimple2(),0,1, false),
                        (StructuralCausalModel) CausalOps.intervention(modelSimple2(),1,1, false)}),

                Arguments.of(modelSingleU(), new StructuralCausalModel[]{
                        (StructuralCausalModel) CausalOps.intervention(modelSingleU(),0,1, false)}),
                Arguments.of(modelSingleU(), new StructuralCausalModel[]{
                        (StructuralCausalModel) CausalOps.intervention(modelSingleU(),1,1, false)}),
                Arguments.of(modelSingleU(), new StructuralCausalModel[]{
                        (StructuralCausalModel) CausalOps.intervention(modelSingleU(),0,1, false),
                        (StructuralCausalModel) CausalOps.intervention(modelSingleU(),1,1, false)})

        );
    }

    @Test
    public  void endoExoLinks() throws IOException {

        StructuralCausalModel model = syntheticModel();

        // Exogenous parents
        assertArrayEquals(model.getExogenousParents(2),  new int[]{5, 6});

        //Edgogenous parents
        assertArrayEquals(model.getEndegenousParents(2),  new int[]{1});

        // Endogenous children
        assertArrayEquals(model.getEndogenousChildren(5),  new int[]{2, 3});
        assertArrayEquals(model.getEndogenousChildren(2),  new int[]{3});


    }



    @Test
    public  void empiricalSet() throws IOException {

        StructuralCausalModel m = syntheticModel();

        HashMap map = m.getEmpiricalMap();
        Set keys = map.keySet();

        // length of components
        Assert.assertEquals(keys.size(), 3);

        // Values in keys
        Assert.assertTrue(map.containsKey(Set.of(1,2,3)));
        Assert.assertTrue(map.containsKey(Set.of(0)));
        Assert.assertTrue(map.containsKey(Set.of(4)));

        // Check the empirical P(1,2,3 | 0)
        double[] expected = new double[] { 0.07, 0.429, 0.055, 0.006, 0.103, 0.103, 0.406, 0.057, 0.097, 0.327, 0.261, 0.003, 0.007, 0.051, 0.001, 0.024 };
        double[] actual = ((BayesianFactor)map.get(Set.of(1,2,3))).getData();
        Assert.assertArrayEquals(expected, actual, 0.001);

    }

    @Test
    public  void components() throws IOException {

        StructuralCausalModel m = syntheticModel();

        // U-components
        assertArrayEquals(new int[]{7}, m.exoConnectComponents().get(0));
        assertArrayEquals(new int[]{5,6}, m.exoConnectComponents().get(1));
        assertArrayEquals(new int[]{8}, m.exoConnectComponents().get(2));

        // C-components
        assertArrayEquals(new int[]{0}, m.endoConnectComponents().get(0));
        assertArrayEquals(new int[]{1,2,3}, m.endoConnectComponents().get(1));
        assertArrayEquals(new int[]{4}, m.endoConnectComponents().get(2));


    }



    @Test
    public  void exoTreeWidth() throws IOException {

        int actual = 0;

        actual = modelSimple().getExogenousTreewidth();
        assertEquals(1, actual, 0);

        actual = modelSimple2().getExogenousTreewidth();
        assertEquals(1, actual, 0);

        actual = modelSingleU().getExogenousTreewidth();
        assertEquals(1, actual, 0);

        actual = syntheticModel().getExogenousTreewidth();
        assertEquals(2, actual, 0);

    }

}
