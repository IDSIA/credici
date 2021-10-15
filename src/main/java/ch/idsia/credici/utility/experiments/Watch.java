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

	public static void stopAndPrint(){
		watch.stop();
		System.out.println("Ellapsed time: "+watch.getTime()+".ms");
	}

	public static long pause(){
		watch.suspend();
		return watch.getTime();
	}

	public static void pauseAndPrint(){
		watch.suspend();
		System.out.println("Ellapsed time: "+watch.getTime()+".ms");
	}

	public static long resume(){
		watch.resume();
		return watch.getTime();
	}

	public static void resumeAndPrint(){
		watch.resume();
		System.out.println("Ellapsed time: "+watch.getTime()+".ms");
	}

}
