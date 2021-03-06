package com.example.lzh.nystagmus.Utils;

import android.support.annotation.NonNull;

import com.example.lzh.nystagmus.Utils.Tool;
import com.example.lzh.nystagmus.Utils.Box;

import org.bytedeco.javacpp.helper.*;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Loader.*;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.javacpp.opencv_highgui.*;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_ml;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.avutil.*;
import org.bytedeco.javacpp.Pointer.*;
import org.bytedeco.javacpp.opencv_core.CvSeqReader;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.Point2dVectorVector;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_highgui.*;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_imgproc.*;
import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_imgcodecs.*;
import org.bytedeco.javacpp.opencv_features2d.*;
import org.bytedeco.javacv.JavaCV.*;

import java.util.List;
import java.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import static android.os.Build.VERSION_CODES.N;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_CCOMP;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.javacpp.opencv_imgproc.cvGetSpatialMoment;
import static org.bytedeco.javacpp.opencv_ml.SVM.C;

/**
 * 与图像处理相关的函数
 * Created by LZH on 2017/7/17.
 */

public class ImgProcess {
    private double EyeRatio;
    private int EyeNum;
    private Mat inimg=new Mat();//输入双眼视频，目前用不到
    private Mat outimg=new Mat();//输出双眼视频，目前也用不到
    private Mat Reye=new Mat();
    private Mat Leye=new Mat();
    private Size size=new Size(9,9);
    private MatVector Rcontours=new MatVector();
    private MatVector Lcontours=new MatVector();
    private double Lmaxarea=0;//左眼最大轮廓
    private int LmaxAreaIndex=0;//左眼最大轮廓下标
    private double Rmaxarea=0;//右眼最大轮廓
    private int RmaxAreaIndex=0;//右眼最大轮廓下标
    private CvRect Rrect=new CvRect();
    private CvRect Lrect=new CvRect();
    private Scalar blue=new Scalar(0,0,255,0);
    private Scalar green=new Scalar(0,255,0,0);
    private Scalar red=new Scalar(255,0,0,0);
    private CvScalar cvblue=new CvScalar(0,0,255,0);
    private CvScalar cvgreen=new CvScalar(0,255,0,0);
    private CvScalar cvred=new CvScalar(255,0,0,0);
    private Mat OriginalLeftEye=new Mat();//用于保存原始图像
    private Mat OriginalRightEye=new Mat();//用于保存原始图像
    public Vector<Box> Lcircles=new Vector<Box>();
    public Vector<Box> Rcircles=new Vector<Box>();
    private IplImage LeyeImage;
    private IplImage ReyeImage;

    /**
     * 默认构造函数
     */
    public ImgProcess()
    {}
    /**
     * 开始参数设置
     * @param leye 输入左眼图像
     * @param reye 输入右眼图像
     * @param eyeratio 眼睛轮廓高宽比，用于闭眼检测
     * @param eyenum 眼睛数目
     */
    public void Start(Mat leye, Mat reye, double eyeratio, int eyenum)
    {
        Reye=new Mat(reye);
        Leye=new Mat(leye);
        EyeRatio=eyeratio;
        EyeNum=eyenum;
        LeyeImage=new IplImage(leye);
        ReyeImage=new IplImage(reye);
        //保存原始图像数据
        OriginalLeftEye=new Mat(leye);
        OriginalRightEye=new Mat(reye);
    }
    /**
     * 输出双眼图像
     * @return 双眼图像
     */
    public Mat Outputimg()
    {
        return outimg;
    }
    /**
     * 输出右眼图像
     * @return 右眼图像
     */
    public Mat OutReye()
    {
        return Reye;
        //return OriginalRightEye;
    }
    /**
     * 输出左眼图像
     * @return 左眼图像
     */
    public Mat OutLeye()
    {
        return Leye;
        //return OriginalLeftEye;
    }
    /**
     * 图像左右分割
     * @param divedeImg 源图像
     * @return ture：分割成功，false：分割失败
     */
    private boolean DivideEye(final Mat divedeImg)
    {
        if(divedeImg.rows()>0&&divedeImg.cols()>0)
        {
            Rect reye_box = new Rect(0, 1, divedeImg.cols()/2, divedeImg.rows() - 1);
            Rect leye_box = new Rect(divedeImg.cols()/2, 1, divedeImg.cols()/2-1, divedeImg.rows() - 1);
            Leye=new Mat(divedeImg,reye_box);
            Reye=new Mat(divedeImg,leye_box);

            return true;
        }
        else
        {
            return false;
        }
    }
    /**
     * 滤波、灰度化等处理，返回图像便于边缘检测
     * @param grayimg0 源图像
     * @return 灰度图像
     */
    private Mat GrayDetect(Mat grayimg0)
    {
        Mat grayout=new Mat();
        Mat grayimg =new Mat(grayimg0);

        opencv_imgproc.cvtColor(grayimg,grayimg,opencv_imgproc.COLOR_RGB2GRAY);//灰度化处理
        opencv_imgproc.medianBlur(grayimg,grayimg,9);//中值滤波
        opencv_imgproc.blur(grayimg,grayimg,size);
        grayout=Binary(grayimg,Tool.RecognitionGrayValue);
        return grayout;
    }
    /**
     * 二值化处理
     * @param binaryimg 源图像
     * @param value 二值化阈值
     * @return 二值化图像
     */
    private Mat Binary(Mat binaryimg, int value)
    {
        Mat binaryout=new Mat();
        opencv_imgproc.threshold(binaryimg,binaryout,value,255,opencv_imgproc.THRESH_BINARY);
        return binaryout;
    }
    /**
     * 二乘法拟合圆
     * @param points 待拟合点的集合
     * @return 拟合圆，包括圆心坐标和半径
     */
    private Box circleLeastFit(Vector<Point> points)
    {
        Box box=new Box(0.0d,0.0d,0.0d);
        long Sum=points.size();
        //如果少于三点，不能拟合圆，直接返回
        if(Sum<3)
        {
            return box;
        }
        int i;
        double X1 = 0;
        double Y1 = 0;
        double X2 = 0;
        double Y2 = 0;
        double X3 = 0;
        double Y3 = 0;
        double X1Y1 = 0;
        double X1Y2 = 0;
        double X2Y1 = 0;

        for (i = 0; i < Sum; ++i)
        {
            X1 += points.get(i).x();
            Y1 += points.get(i).y();
            X2 += points.get(i).x()*points.get(i).x();
            Y2 += points.get(i).y()*points.get(i).y();
            X3 += points.get(i).x()*points.get(i).x()*points.get(i).x();
            Y3 += points.get(i).y()*points.get(i).y()*points.get(i).y();
            X1Y1 += points.get(i).x()*points.get(i).y();
            X1Y2 += points.get(i).x()*points.get(i).y()*points.get(i).y();
            X2Y1 += points.get(i).x()*points.get(i).x()*points.get(i).y();
        }

        double C, D, E, G, H, N;
        double a, b, c;
        N = points.size();
        C = N*X2 - X1*X1;
        D = N*X1Y1 - X1*Y1;
        E = N*X3 + N*X1Y2 - (X2 + Y2)*X1;
        G = N*Y2 - Y1*Y1;
        H = N*X2Y1 + N*Y3 - (X2 + Y2)*Y1;
        a = (H*D - E*G) / (C*G - D*D);
        b = (H*C - E*D) / (D*D - G*C);
        c = -(a*X1 + b*Y1 + X2 + Y2) / N;

        box.setX(a/(-2));
        box.setY(b/(-2));
        box.setR(sqrt(a*a + b*b - 4 * c) / 2);
        return box;
    }
    /**
     * 边缘检测
     * @param edgeimg 检测图像
     * @return 边缘图像
     */
    private Mat EdgeDetect(Mat edgeimg)
    {
        Mat edgeout=new Mat();
        opencv_imgproc.Canny(edgeimg,edgeout,100,250,3,false);
        return edgeout;
    }
    /**
     * 绘制源
     * @param circles 需要绘制的圆的集合
     * @param midImage 源图像
     */
    private void PlotC(Vector<Box> circles,IplImage midImage)
    {
        for(int i=0;i<circles.size();++i)
        {
            CvPoint center=new CvPoint((int)Math.round(circles.get(i).getX()),(int)Math.round(circles.get(i).getY()));
            int radius=(int)circles.get(i).getR();
            opencv_imgproc.cvCircle(midImage,center,1,cvblue,-1,8,0);//画圆心
            opencv_imgproc.cvCircle(midImage,center,radius,cvred,1,8,0);//画圆轮廓
        }
    }
    /**
     * 眼睛图像识别处理
     */
    public void ProcessSeparate()
    {
        Mat Rgryaimg;
        Mat Lgrayimg;
        Mat Redgeimg;
        Mat Ledgeimg;
        Mat Rhiberarchy=new Mat();
        Mat Lhiberarchy=new Mat();
        double temparea;
        boolean IsLeye=false;
        boolean IsReye=false;

        CvMemStorage Lstorage=CvMemStorage.create();
        CvMemStorage Rstorage=CvMemStorage.create();
        CvSeq cvLcontour=new CvSeq(null);
        CvSeq cvRcontour=new CvSeq(null);
        CvSeq cvtempLcontour=new CvSeq(null);
        CvSeq cvtempRcontour=new CvSeq(null);
        CvSeq cvLcontourKeep=new CvSeq(null);
        CvSeq cvRcontourKeep=new CvSeq(null);
        IplImage LmatImage;
        IplImage RmatImage;

        if(EyeNum==Tool.NOT_LEYE||EyeNum==Tool.ALL_EYE||EyeNum==Tool.VEDIO_EYE)
        {
            //此时有右眼
            Rgryaimg=GrayDetect(Reye);
            Redgeimg=EdgeDetect(Rgryaimg);
            RmatImage=new IplImage(Redgeimg);
            opencv_imgproc.cvFindContours(RmatImage,Rstorage,cvRcontour,Loader.sizeof(CvContour.class), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE);
            cvRcontourKeep=new CvSeq(cvRcontour);
            //opencv_imgproc.findContours(Redgeimg,Rcontours,Rhiberarchy,opencv_imgproc.RETR_CCOMP,opencv_imgproc.CHAIN_APPROX_NONE);
            IsReye=true;
        }
        if(EyeNum==Tool.NOT_REYE||EyeNum==Tool.ALL_EYE||EyeNum==Tool.VEDIO_EYE||EyeNum==Tool.VEDIO_ONLY_EYE)
        {
            //此时有左眼
            Lgrayimg=GrayDetect(Leye);
            Ledgeimg=EdgeDetect(Lgrayimg);
            LmatImage=new IplImage(Ledgeimg);
            opencv_imgproc.cvFindContours(LmatImage,Lstorage,cvLcontour,Loader.sizeof(CvContour.class), CV_RETR_CCOMP, CV_CHAIN_APPROX_NONE);
            cvLcontourKeep=new CvSeq(cvLcontour);
            //opencv_imgproc.findContours(Ledgeimg,Lcontours,Lhiberarchy,opencv_imgproc.RETR_CCOMP,opencv_imgproc.CHAIN_APPROX_NONE);
            IsLeye=true;
        }
        if(!cvLcontour.isNull()&&cvLcontour.elem_size()>0&&IsLeye)
        {
            //左眼有轮廓
            while (cvLcontour != null && !cvLcontour.isNull())
            {
                temparea=opencv_imgproc.cvContourArea(cvLcontour);
                if(temparea>Lmaxarea)
                {
                    Lmaxarea=temparea;
                    cvtempLcontour=cvLcontour;
                    continue;
                }
                cvLcontour=cvLcontour.h_next();
            }
            if(!cvtempLcontour.isNull())
            {
                Vector<Point> leftPoints=new Vector<Point>();
                for(int i=0;i<cvtempLcontour.total();++i)
                {
                    Point p=new Point(opencv_core.cvGetSeqElem(cvtempLcontour,i));
                    leftPoints.add(p);
                }
                //闭眼检测
                Lrect=opencv_imgproc.cvBoundingRect(cvtempLcontour);
                if((Lrect.width()/(float)Lrect.height())<EyeRatio&&(Lrect.height()/(float)Lrect.width())<EyeRatio&&Lrect.width()>0&&Lrect.height()>0)
                {
                    //此处有BUG
                    Box Lbox=circleLeastFit(leftPoints);//左眼拟合圆检测
                    if(Lbox.getR()!=0)
                    {
                        //如果半径不为0
                        Lcircles.add(Lbox);
                    }
                }
            }

        }
        if(!cvRcontour.isNull()&&cvRcontour.elem_size()>0&&IsReye)
        {
            //右眼有轮廓
            while (cvRcontour!=null&&!cvRcontour.isNull())
            {
                temparea=opencv_imgproc.cvContourArea(cvRcontour);
                if(temparea>Rmaxarea)
                {
                    Rmaxarea=temparea;
                    cvtempRcontour=cvRcontour;
                    continue;
                }
                cvRcontour=cvRcontour.h_next();
            }
            if(!cvtempRcontour.isNull())
            {
                Vector<Point> rightPoints=new Vector<Point>();
                for(int i=0;i<cvtempRcontour.total();++i)
                {
                    Point p=new Point(opencv_core.cvGetSeqElem(cvtempRcontour,i));
                    rightPoints.add(p);
                }
                //闭眼检测
                Rrect=opencv_imgproc.cvBoundingRect(cvtempRcontour);
                if((Rrect.width()/(float)Rrect.height())<EyeRatio&&(Rrect.height()/(float)Rrect.width())<EyeRatio&&Rrect.width()>0&&Rrect.height()>0)
                {
                    Box Rbox=circleLeastFit(rightPoints);//左眼拟合圆检测
                    if(Rbox.getR()!=0)
                    {
                        //如果半径不为0
                        Rcircles.add(Rbox);
                    }
                }
            }
        }
        if(Lcircles.size()>0)
        {
            PlotC(Lcircles,LeyeImage);//绘制左眼圆心和圆轮廓
        }
        if(Rcircles.size()>0)
        {
            PlotC(Rcircles,ReyeImage);//绘制右眼圆心和圆轮廓
        }

        if(IsLeye)
        {
            //opencv_imgproc.drawContours(Leye,Lcontours,LmaxAreaIndex,green);//使用opencv绘制轮廓
            //opencv_imgproc.cvDrawContours(LeyeImage,cvLcontourKeep,cvgreen,cvgreen,1);//使用javacv绘制轮廓
        }
        if(IsReye)
        {
            //opencv_imgproc.drawContours(Reye,Rcontours,RmaxAreaIndex,green);//使用opencv绘制轮廓
            //opencv_imgproc.cvDrawContours(ReyeImage,cvRcontourKeep,cvgreen,cvgreen,1);//使用javacv绘制轮廓
        }
        Reye=new Mat(ReyeImage);
        Leye=new Mat(LeyeImage);


    }
}
