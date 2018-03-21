package it.gerdavax.brickpi3;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * BrickPi3 Library for Android Things
 *
 * Created by gerdavax on 09/03/2017.
 *
 * Released under Apache 2.0 License
 */

public final class BrickPi4 {
    private static final BrickPi3 _instance = new BrickPi3();
    public static final String SPI_DEVICE = "SPI0.1";
    public static final int SPEED = 500000;
    public enum MOTOR_PORT {A, B, C, D}
    public enum SENSOR_PORT {S1, S2, S3, S4}
    public enum SENSOR_TYPE {
        NONE, I2C, CUSTOM,
        TOUCH, NXT_TOUCH, EV3_TOUCH,
        NXT_LIGHT_ON, NXT_LIGHT_OFF,
        NXT_COLOR_RED, NXT_COLOR_GREEN, NXT_COLOR_BLUE, NXT_COLOR_FULL, NXT_COLOR_OFF,
        NXT_ULTRASONIC,
        EV3_GYRO_ABS, EV3_GYRO_DPS, EV3_GYRO_ABS_DPS,
        EV3_COLOR_REFLECTED, EV3_COLOR_AMBIENT, EV3_COLOR_COLOR, EV3_COLOR_RAW_REFLECTED, EV3_COLOR_COLOR_COMPONENTS,
        EV3_ULTRASONIC_CM, EV3_ULTRASONIC_INCHES, EV3_ULTRASONIC_LISTEN,
        EV3_INFRARED_PROXIMITY, EV3_INFRARED_SEEK, EV3_INFRARED_REMOTE
    }
    public enum SENSOR_STATE {VALID_DATA, NOT_CONFIGURED, CONFIGURING, NO_DATA}
    public static boolean LOG_ENABLED = true;
    public static String TAG = "BrickPi3";
    private static final byte OK = (byte) 0xA5;
    private static final byte MESSAGE_READ_MANUFACTURER = 0x01;
    private static final byte MESSAGE_READ_DEVICE_NAME = 0x02;
    private static final byte MESSAGE_READ_HARDWARE_VERSION = 0x03;
    private static final byte MESSAGE_READ_FIRMWARE_VERSION = 0x04;
    private static final byte MESSAGE_READ_ID = 0x05;
    private static final byte MESSAGE_SET_LED = 0x06;
    private static final byte MESSAGE_READ_VOLTAGE_3V3 = 0x7;
    private static final byte MESSAGE_READ_VOLTAGE_5V = 0x8;
    private static final byte MESSAGE_READ_VOLTAGE_9V = 0x9;
    private static final byte MESSAGE_READ_VOLTAGE_VCC = 0x0A; // 10
    private static final byte MESSAGE_SENSOR_TYPE = 0x14; //20
    private static final byte MESSAGE_READ_SENSOR = 0x18; //24;
    private static final byte MESSAGE_SET_MOTOR_SPEED = 0x1C; // 28
    private static final byte MESSAGE_SET_MOTOR_POSITION = 0x20; // 32
    private static final byte MESSAGE_SET_MOTOR_POSITION_KP = 0x24; // 36
    private static final byte MESSAGE_SET_MOTOR_POSITION_KD = 0x28; // 40
    private static final byte MESSAGE_SET_MOTOR_DPS = 0x2C; // 44
    private static final byte MESSAGE_SET_MOTOR_DPS_KP = 0x30; // 48
    private static final byte MESSAGE_SET_MOTOR_DPS_KD = 0x34; // 52
    private static final byte MESSAGE_SET_OFFSET_MOTOR_ENCODER = 0x38; // 56
    private static final byte MESSAGE_READ_OFFSET_MOTOR_ENCODER = 0x3C; // 60
    private static final byte MESSAGE_I2C_TRANSACT = 0x40; // 64
    private static final byte MESSAGE_SET_MOTOR_LIMITS = 0x44; // 68
    private static final byte MESSAGE_READ_MOTOR_STATUS = 0x48; // 72

    private PeripheralManagerService mManager;
    private SpiDevice mSPI;
    private String mHardwareVersion;
    private String mFirmwareVersion;
    private HashMap<SENSOR_PORT, SENSOR_TYPE> mSensors = new HashMap<>();

    //private. This ensure that there is only one instance
    private BrickPi3() {

    }

    public static BrickPi3 getInstance() {
        return _instance;
    }

    public void open() throws IOException {
        mManager = new PeripheralManagerService();

        resetPINs();

        configureSPIDevice();
    }

    public void close() {
        try {
            mSPI.close();
        } catch (Exception e) {
            // nothing to do...

            e.printStackTrace();
        }
    }

    public void setLED(int value) throws IOException {
        if (value < 0) {
            value = 0;
        } else if (value > 100) {
            value = 100;
        }

        d("LED value: " + value);

        sendRequest(new byte[] {MESSAGE_SET_LED, (byte) value}, 0);
    }


    public String getDeviceName() throws IOException {
        byte[] response = sendRequest((byte) 2, 20);

        StringBuffer buffer = new StringBuffer();

        for (int i=0; i < response.length; i++) {
            if (response[i] != 0) {
                buffer.append((char) response[i]);
            }
        }

        d("Device name: " + buffer);
        d("Response RAW: " + Arrays.toString(response));

        return buffer.toString();
    }

    public String getManufacturerName() throws IOException {
        byte[] response = sendRequest((byte) 1, 20);

        StringBuffer buffer = new StringBuffer();

        for (int i=0; i < response.length; i++) {
            if (response[i] != 0) {
                buffer.append((char) response[i]);
            }
        }

        d("Manufacturer: " + buffer);
        d("Response RAW: " + Arrays.toString(response));

        return buffer.toString();
    }

    /*
     * Returns the hardware revision of the board
     */
    public String getHardwareVersion() throws IOException {
        if (mHardwareVersion == null) {
            byte[] response = sendRequest(MESSAGE_READ_HARDWARE_VERSION, 4);

            int hardwareVersion32bit = ByteBuffer.wrap(response).getInt();
            String hardwareVersionString = Integer.toString(hardwareVersion32bit);

            mHardwareVersion = hardwareVersionString.charAt(0) + "." + hardwareVersionString.charAt(3) + "." + hardwareVersionString.charAt(6);
        }

        return mHardwareVersion;
    }

    // to be completed...
    public String getFirmwareVersion() throws IOException {
        if (mFirmwareVersion == null) {
            byte[] response = sendRequest(MESSAGE_READ_FIRMWARE_VERSION, 4);

            int firmwareVersion32bit = ByteBuffer.wrap(response).getInt();
            String firmwareVersionString = Integer.toString(firmwareVersion32bit);

            mFirmwareVersion = firmwareVersionString.charAt(0) + "." + firmwareVersionString.charAt(3) + "." + firmwareVersionString.charAt(6);
        }

        return mFirmwareVersion;
    }

    /*
     *
     */
    public byte getMotorStatus(MOTOR_PORT port) throws IOException {
        return 0;
    }

    // to be completed....
    public String getID() throws IOException {
        return null;
    }

    public int getBatteryVoltage() throws IOException {
        return readVoltage(MESSAGE_READ_VOLTAGE_VCC);
    }

    public int get3v3Voltage() throws IOException {
        return readVoltage(MESSAGE_READ_VOLTAGE_3V3);
    }

    public int get5vVoltage() throws IOException {
        return readVoltage(MESSAGE_READ_VOLTAGE_5V);
    }

    public int get9vVoltage() throws IOException {
        return readVoltage(MESSAGE_READ_VOLTAGE_9V);
    }

    private int readVoltage(byte address) throws IOException {
        byte[] request = {address};

        byte[] response = sendRequest(request, 2);

        int hi = response[0] & 0xFF;
        int lo = response[1] & 0xFF;

        int mv = (hi << 8) + lo;

        return mv;
    }

    public void resetAllPeripherals() throws IOException {

    }

    public void setSensorType(SENSOR_PORT port, SENSOR_TYPE type) throws IOException {
        byte[] request = {(byte) (MESSAGE_SENSOR_TYPE + port.ordinal()), (byte) (type.ordinal() + 1)};

        d("Set sensor type request: " + Arrays.toString(request));

        sendRequest(request, 0);

        mSensors.put(port, type);
    }

    /*
     * Reads sensor raw data
     */
    public byte[] readSensor(SENSOR_PORT port) throws IOException {
        byte[] request = { (byte) (MESSAGE_READ_SENSOR + port.ordinal())};

        // [0] sensor, [1] state, [2] value
        byte[] response = null;

        SENSOR_TYPE sensor = mSensors.get(port);

        switch (sensor) {
            case TOUCH:
            case NXT_TOUCH:
            case EV3_TOUCH:
            case NXT_ULTRASONIC:
            case EV3_COLOR_REFLECTED:
            case EV3_COLOR_AMBIENT:
            case EV3_COLOR_COLOR:
            case EV3_ULTRASONIC_LISTEN:
            case EV3_INFRARED_PROXIMITY:
                response = sendRequest(request, 3);
                d(Arrays.toString(response));
                break;
            case NXT_COLOR_FULL:
                response = sendRequest(request, 8);
                break;
            case NXT_LIGHT_ON:
            case NXT_LIGHT_OFF:
            case NXT_COLOR_RED:
            case NXT_COLOR_GREEN:
            case NXT_COLOR_BLUE:
            case NXT_COLOR_OFF:
                response = sendRequest(request, 4);
                break;
            case EV3_GYRO_ABS:
            case EV3_GYRO_DPS:
                response = sendRequest(request, 4);
                break;
            case EV3_ULTRASONIC_CM:
            case EV3_ULTRASONIC_INCHES:
                response = sendRequest(request, 4);
                break;

        }

        d("Sensor: " + Arrays.toString(response) + " on port " + port);

        return response;
    }

    // assumes that there is a touch sensor
    public boolean isPressed(SENSOR_PORT port) throws IOException {
        //verifySensor(port, SENSOR_TYPE.TOUCH);
        verifySensor(port, SENSOR_TYPE.NXT_TOUCH);
        //verifySensor(port, SENSOR_TYPE.EV3_TOUCH);

        return readSensor(port)[2] == 1;
    }

    //EV3 ultrasonic CM seems OK
    public int getDistanceInCm(SENSOR_PORT port) throws IOException {
        verifySensor(port, SENSOR_TYPE.NXT_ULTRASONIC, SENSOR_TYPE.EV3_ULTRASONIC_CM);

        if (mSensors.get(port) == SENSOR_TYPE.NXT_ULTRASONIC) {
            return readSensor(port)[2];
        } else if (mSensors.get(port) == SENSOR_TYPE.EV3_ULTRASONIC_CM) {
            byte[] response = readSensor(port);

            int hi = response[2] & 0xFF;
            int lo = response[3] & 0xFF;

            int value = (hi << 8) + lo;

            return value;
        } else {
            return 0;
        }
    }

    // seems OK
    public int readReflectedLight(SENSOR_PORT port) throws IOException {
        verifySensor(port, SENSOR_TYPE.NXT_LIGHT_ON);

        return readLight(port);
    }

    // il risultato non mi piace (e mette il dubbio anche la luce riflessa)
    public int readAmbientLight(SENSOR_PORT port) throws IOException {
        verifySensor(port, SENSOR_TYPE.NXT_LIGHT_OFF);

        return readLight(port);
    }

    private int readLight(SENSOR_PORT port) throws IOException {
        byte[] response = readSensor(port);

        int light = (((response[2] & 0xFF ) << 8) & 0xFF00) + (response[3] & 0xFF);

        return light;
    }

    // absolute rotation position in degrees
    public int getAbsoluteRotationPosition(SENSOR_PORT port) throws IOException {
        byte[] response = readSensor(port);

        int hi = response[2] & 0xFF;
        int lo = response[3] & 0xFF;

        int value = (hi << 8) + lo;

        if ((value & 0x1000) != 0) {
            value = value - 0x10000;
        }

        return value;
    }

    // rotation rate in degrees per second
    public int getRotationDegreesPerSecond(SENSOR_PORT port) throws IOException {
        byte[] response = readSensor(port);

        int hi = response[2] & 0xFF;
        int lo = response[3] & 0xFF;

        int value = (hi << 8) + lo;

        if ((value & 0x1000) != 0) {
            value = value - 0x10000;
        }

        return value;
    }

    // rotation rate in degrees per second
    public int getRotationRate(SENSOR_PORT port) throws IOException {
        return 0;
    }

    public void setMotorSpeed(MOTOR_PORT port, byte speed) throws IOException {
        if (speed < -100) {
            speed = -100;
        } else if (speed > 100) {
            speed = 100;
        }

        byte[] request = {(byte) (MESSAGE_SET_MOTOR_SPEED + port.ordinal()), speed};

        sendRequest(request, 0);
    }

    // A 32-bit signed value to specify motor target position in degrees. -2,147,483,648 to 2,147,483,647
    public void setMotorPosition(MOTOR_PORT port, int position) throws IOException {
        byte hi = (byte) ((position >> 24) & 0xFF);
        byte mid_hi = (byte) ((position >> 16) & 0xFF);
        byte mid_lo = (byte) ((position >> 8) & 0xFF);
        byte lo = (byte) (position & 0xFF);

        byte[] request = {(byte) (MESSAGE_SET_MOTOR_POSITION + port.ordinal()), hi, mid_hi, mid_lo, lo};

        sendRequest(request, 0);
    }

    /*
    if encoder & 0x80000000: # MT was 0x10000000, but I think it should be 0x80000000
            encoder = int(encoder - 0x100000000)
        #if encoder > 2147483647:
#    encoder -= 4294967295
     */
    public int getMotorEncoder(MOTOR_PORT port) throws IOException {
        byte[] response = sendRequest(MESSAGE_READ_OFFSET_MOTOR_ENCODER, 4);

        int hi = response[0] & 0xFF;
        int mid_hi = response[1] & 0xFF;
        int mid_lo = response[2] & 0xFF;
        int lo = response[3] & 0xFF;

        int offset = (hi << 24) + (mid_hi << 16) + (mid_lo << 8) + lo;

        return offset;
    }

    public void setOffsetMotorEncoder(MOTOR_PORT port, int position) throws IOException {
        byte hi = (byte) ((position >> 24) & 0xFF);
        byte mid_hi = (byte) ((position >> 16) & 0xFF);
        byte mid_lo = (byte) ((position >> 8) & 0xFF);
        byte lo = (byte) (position & 0xFF);

        byte[] request = {(byte) (MESSAGE_SET_OFFSET_MOTOR_ENCODER + port.ordinal()), hi, mid_hi, mid_lo, lo};

        sendRequest(request, 0);
    }

    private void verifySensor(SENSOR_PORT port, SENSOR_TYPE... expectedSensors) {
        boolean found = false;

        SENSOR_TYPE installedSensor = mSensors.get(port);

        for (SENSOR_TYPE type: expectedSensors) {
            d("Sensor: " + type);

            if (installedSensor == type) {
                found = true;
            }
        }

        if (! found) {
            throw new IllegalArgumentException();
        }
    }

    private void sendRequest(byte req) throws IOException {

    }

    /*
     * Sends a request with a single byte as payload (no parameters command)
     */
    private byte[] sendRequest(byte req, int padding) throws IOException {
        return sendRequest(new byte[] {req}, padding);
    }

    private byte[] sendRequest(byte[] req, int padding) throws IOException {

        byte[] request = new byte[1 + req.length + 2 + padding];

        // default address


        Arrays.fill(request, (byte) 0x00);
        request[0] = 0x01;
        System.arraycopy(req, 0, request, 1, req.length);

        byte[] resp = new byte[request.length];

        mSPI.transfer(request, resp, request.length);

        if ((byte) (resp[3] & 0xFF) != (byte) OK) {
            throw new IOException();
        }

        byte[] response = new byte[resp.length - 4];

        System.arraycopy(resp, 4, response, 0, response.length);

        return response;
    }

    private void resetPINs() {
        try {

            List<String> portList = mManager.getGpioList();
            if (portList.isEmpty()) {
                //Log.i(TAG, "No GPIO port available on this device.");
            } else {
                //Log.i(TAG, "List of available ports: " + portList);

                // BCM12, BCM13, BCM16, BCM17, BCM18, BCM19, BCM20, BCM21, BCM22, BCM23, BCM24, BCM25, BCM26, BCM27, BCM4, BCM5, BCM6

                for (String gpioName: portList) {
                    Gpio gpio;

                    if (gpioName.equals("BCM4") || gpioName.equals("BCM5") || gpioName.equals("BCM6")) {
                        gpio = mManager.openGpio(gpioName);
                        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                        gpio.setActiveType(Gpio.ACTIVE_HIGH);
                        gpio.setValue(false);
                        gpio.close();
                    } else {
                        gpio = mManager.openGpio(gpioName);
                        gpio.setDirection(Gpio.DIRECTION_IN);
                        gpio.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureSPIDevice() throws IOException {
        mSPI = mManager.openSpiDevice(SPI_DEVICE);

        mSPI.setMode(SpiDevice.MODE0);

        mSPI.setFrequency(SPEED);          // 500KHz
        mSPI.setBitsPerWord(8);             // 8 BPW
        mSPI.setBitJustification(false);    // MSB first
    }

    private void d(String message) {
        if (LOG_ENABLED && TAG != null) {
            Log.d(TAG, message);
        }
    }
}
