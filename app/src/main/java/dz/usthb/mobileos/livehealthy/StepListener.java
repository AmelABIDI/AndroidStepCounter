package dz.usthb.mobileos.livehealthy;
//This interface will listen to alerts about steps being detected
// Will listen to step alerts
public interface StepListener {

    public void step(long timeNs);

}
