package ch.idsia.credici.utility.experiments;

import org.apache.commons.lang3.time.StopWatch;

public class Watch {
	public static StopWatch watch = new StopWatch();

	public static void start(){
		watch.reset();
		watch.start();
	}

	public static long stop(){
		watch.stop();
		return watch.getTime();
	}
}
