package gotapps.appresist;

/**
 * Created by Fenta Rizky on 09/12/2017.
 */
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class ResistorImageProcessor {
    private static final int NUM_CODES = 10;

    // HSV colour bounds/ batas warna deteksi
    private static final Scalar COLOR_BOUNDS[][] = {
            { new Scalar(0, 0, 0),   new Scalar(180, 250, 50) },    // black
            { new Scalar(0, 90, 10), new Scalar(15, 250, 100) },    // brown
            { new Scalar(0, 0, 0),   new Scalar(0, 0, 0) },         // red (defined by two bounds)
            { new Scalar(4, 100, 100), new Scalar(9, 250, 150) },   // orange
            { new Scalar(20, 130, 100), new Scalar(30, 250, 160) }, // yellow
            { new Scalar(45, 50, 60), new Scalar(72, 250, 150) },   // green
            { new Scalar(80, 50, 50), new Scalar(106, 250, 150) },  // blue
            { new Scalar(130, 40, 50), new Scalar(155, 250, 150) }, // purple
            { new Scalar(0,0, 50), new Scalar(180, 50, 80) },       // gray
            { new Scalar(0, 0, 90), new Scalar(180, 15, 140) }      // white
    };

    // warna merah yang terdapat pada HSV, definisikan 2 range warna merah
    private static Scalar LOWER_RED1 = new Scalar(0, 65, 100);
    private static Scalar UPPER_RED1 = new Scalar(2, 250, 150);
    private static Scalar LOWER_RED2 = new Scalar(171, 65, 50);
    private static Scalar UPPER_RED2 = new Scalar(180, 250, 150);
    //inisialisasi awal untuk menaruh nilai dari masing masing ring
    private SparseIntArray _locationValues = new SparseIntArray(4);
    //pemrosessan frame yang ditangkap oleh camera
    public Mat processFrame(CvCameraViewFrame frame)
    {
        Mat imageMat = frame.rgba();
        int cols = imageMat.cols();
        int rows = imageMat.rows();
        //pemrosensan Region Of Interest yang ditunjukaan oleh garis indikator warna merah
        Mat subMat = imageMat.submat(rows/2, rows/2+30, cols/2 - 50, cols/2 + 50);
        Mat filteredMat = new Mat();
        Imgproc.cvtColor(subMat, subMat, Imgproc.COLOR_RGBA2BGR);
        Imgproc.bilateralFilter(subMat, filteredMat, 5, 80, 80);
        Imgproc.cvtColor(filteredMat, filteredMat, Imgproc.COLOR_BGR2HSV);

        //lakukan pencarian lokasi ring.
        findLocations(filteredMat);
        //check apabila lokasi ke 4 ring telah ditemukan
        if(_locationValues.size() >= 3)
        {
            //baca nilai ring pertama
            int k_tens = _locationValues.keyAt(0);
            //baca nilai ring kedua
            int k_units = _locationValues.keyAt(1);
            //baca nilai ring ketiga
            int k_power = _locationValues.keyAt(2);
            //lakukan kalkulasi
            int value = 10*_locationValues.get(k_tens) + _locationValues.get(k_units);
            value *= Math.pow(10, _locationValues.get(k_power));
            //tampilkan nilai asli resistor
            String valueStr;
            if(value >= 1e3 && value < 1e6)
                valueStr = String.valueOf(value/1e3) + " KOhm";
            else if(value >= 1e6)
                valueStr = String.valueOf(value/1e6) + " MOhm";
            else
                valueStr = String.valueOf(value) + " Ohm";

//            if(value <= 1e9)
//                Core.putText(imageMat, valueStr, new Point(10, 100), Core.FONT_HERSHEY_COMPLEX,
//                        2, new Scalar(255, 0, 0, 255), 3);
        }

        Scalar color = new Scalar(255, 0, 0, 255);
//        Core.line(imageMat, new Point(cols/2 - 50, rows/2), new Point(cols/2 + 50, rows/2 ), color, 2);
        return imageMat;
    }

    // mencari kontur dari masing masing ring berdasarkan centroid resistor
    private void findLocations(Mat searchMat)
    {
        _locationValues.clear();
        SparseIntArray areas = new SparseIntArray(4);

        for(int i = 0; i < NUM_CODES; i++)
        {
            Mat mask = new Mat();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();

            if(i == 2)
            {
                // combine the two ranges for red
                Core.inRange(searchMat, LOWER_RED1, UPPER_RED1, mask);
                Mat rmask2 = new Mat();
                Core.inRange(searchMat, LOWER_RED2, UPPER_RED2, rmask2);
                Core.bitwise_or(mask, rmask2, mask);
            }
            else
                Core.inRange(searchMat, COLOR_BOUNDS[i][0], COLOR_BOUNDS[i][1], mask);

            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            for (int contIdx = 0; contIdx < contours.size(); contIdx++)
            {
                int area;
                if ((area = (int)Imgproc.contourArea(contours.get(contIdx))) > 20)
                {
                    Moments M = Imgproc.moments(contours.get(contIdx));
                    int cx = (int) (M.get_m10() / M.get_m00());

                    // jika warna terpisah menjadi beberapa kontur
                    // ambil area terbesar sebagai centroid nya
                    boolean shouldStoreLocation = true;
                    for(int locIdx = 0; locIdx < _locationValues.size(); locIdx++)
                    {
                        if(Math.abs(_locationValues.keyAt(locIdx) - cx) < 10)
                        {
                            if (areas.get(_locationValues.keyAt(locIdx)) > area)
                            {
                                shouldStoreLocation = false;
                                break;
                            }
                            else
                            {
                                _locationValues.delete(_locationValues.keyAt(locIdx));
                                areas.delete(_locationValues.keyAt(locIdx));
                            }
                        }
                    }

                    if(shouldStoreLocation)
                    {
                        areas.put(cx, area);
                        _locationValues.put(cx, i);
                    }
                }
            }
        }
    }
}
