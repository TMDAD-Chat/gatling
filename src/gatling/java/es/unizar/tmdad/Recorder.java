package es.unizar.tmdad;

import io.gatling.recorder.GatlingRecorder;
import io.gatling.recorder.config.RecorderPropertiesBuilder;
import scala.Option;

public class Recorder {
    public static void main(String[] args) {
        RecorderPropertiesBuilder props = new RecorderPropertiesBuilder()
                .simulationsFolder(IDEPathHelper.mavenSourcesDirectory.toString())
                .resourcesFolder(IDEPathHelper.mavenResourcesDirectory.toString())
                .simulationPackage("es.unizar.tmdad.simulations");

        GatlingRecorder.fromMap(props.build(), Option.apply(IDEPathHelper.recorderConfigFile));
    }
}