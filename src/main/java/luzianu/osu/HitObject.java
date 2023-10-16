package luzianu.osu;

public class HitObject {
    public int _column;

    public int x;
    public int y;
    public int time;
    public int type;
    public int hitSound;
    public int endTime;
    public String hitSample;

    public HitObject(int x, int y, int time, int type, int hitSound, int endTime, String hitSample) {
        this.x = x;
        this.y = y;
        this.time = time;
        this.type = type;
        this.hitSound = hitSound;
        this.endTime = endTime;
        this.hitSample = hitSample;

        _column = (int) Math.max(0, Math.min(x * 7 / 512.0, 7 - 1));
    }

    private static int[] column_map = new int[] { 36, 109, 182, 255, 328, 401, 474 };

    public static HitObject regular(int column, int time) {
        return new HitObject(column_map[column], 192, time, 1, 0, Integer.MIN_VALUE, "0:0:0:0:");
    }

    public static HitObject hold(int column, int time, int endTime) {
        return new HitObject(column_map[column], 192, time, 128, 0, endTime, "0:0:0:0:");
    }

    @Override
    public String toString() {
        if (type == 1) {
            return String.format("%d,%d,%d,%d,%d,%s", x, y, time, type, hitSound, hitSample);
        }
        return String.format("%d,%d,%d,%d,%d,%d,%s", x, y, time, type, hitSound, endTime, hitSample);
    }

}
