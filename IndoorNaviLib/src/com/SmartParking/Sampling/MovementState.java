package com.SmartParking.Sampling;

public enum MovementState {
	// acc is 1, then sampling time is 5000ms
	Idle(0.08f, 5000), SlowWalking(2, 4000), Walking(3, 3000), SlowRunning(4, 2000), Running(5,1000);

	private final float correlatedAccelaration;
	private final Integer samplingBufferTime;

	MovementState(float correlatedAccelaration, int samplingBufferTime) {
		this.correlatedAccelaration = correlatedAccelaration;
		this.samplingBufferTime = samplingBufferTime;
	}
	
	public float getAcc()
	{
		return this.correlatedAccelaration;
	}
	
	public int getSamplingBufferTime()
	{
		return this.samplingBufferTime;
	}
}
