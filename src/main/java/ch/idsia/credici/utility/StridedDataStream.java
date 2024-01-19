package ch.idsia.credici.utility;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator.OfDouble;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class StridedDataStream implements DoubleStream {

	@Override
	public boolean isParallel() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DoubleStream unordered() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream onClose(Runnable closeHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public DoubleStream filter(DoublePredicate predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream map(DoubleUnaryOperator mapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntStream mapToInt(DoubleToIntFunction mapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LongStream mapToLong(DoubleToLongFunction mapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream distinct() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream sorted() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream peek(DoubleConsumer action) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream limit(long maxSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream skip(long n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void forEach(DoubleConsumer action) {
		// TODO Auto-generated method stub

	}

	@Override
	public void forEachOrdered(DoubleConsumer action) {
		// TODO Auto-generated method stub

	}

	@Override
	public double[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double reduce(double identity, DoubleBinaryOperator op) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public OptionalDouble reduce(DoubleBinaryOperator op) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double sum() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public OptionalDouble min() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OptionalDouble max() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long count() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public OptionalDouble average() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleSummaryStatistics summaryStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean anyMatch(DoublePredicate predicate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allMatch(DoublePredicate predicate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean noneMatch(DoublePredicate predicate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public OptionalDouble findFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OptionalDouble findAny() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<Double> boxed() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream sequential() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DoubleStream parallel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OfDouble iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.util.Spliterator.OfDouble spliterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
