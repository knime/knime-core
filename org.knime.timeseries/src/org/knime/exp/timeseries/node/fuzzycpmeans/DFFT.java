package org.knime.exp.timeseries.node.fuzzycpmeans;

/**
 * 
 * Quellen aus Internet, Original von Jon Squire
 * 
 * Schnelle, diskrete Fourier-Transformation.
 */

public class DFFT {

	/**
	 * @param ts
	 *            Zeitreihe, abwechselnd Real- und Imaginärteil, Funktionswerte
	 *            einer reellen Zeitreihe also bei Index 0, 1, 3, 5 etc.
	 *            eintragen
	 * @return Frequenzspektrum, abwechselnd Real- und Imaginärteil
	 */
	public static void fft(double ts[]) {
		fft(ts, true);
	}

	public static void ifft(double ts[]) {
		fft(ts, false);
	}

	public static double[] realToComplex(double ts[]) {
		double[] freq = new double[2*ts.length];
		for (int i=0;i<ts.length;++i) freq[2*i]=ts[i];
		return freq;
	}

	private static void fft(double A[], boolean analyse)
	// real,imag real,imag ... trans=1.0 for FFT
	{
		final double trans = (analyse) ? 1.0 : -1.0;
		double tmpr, tmpi;
		double wxr, wxi;
		double wr, wi;
		int i, js, ix, m, isp, mmax;
		double ph1;
		int n = A.length / 2;

		js = 1;
		for (ix = 1; ix < n; ix++) // reorder data
		{
			if (js > ix) {
				tmpr = A[2 * (js - 1)];
				tmpi = A[2 * (js - 1) + 1];
				A[2 * (js - 1)] = A[2 * (ix - 1)];
				A[2 * (js - 1) + 1] = A[2 * (ix - 1) + 1];
				A[2 * (ix - 1)] = tmpr;
				A[2 * (ix - 1) + 1] = tmpi;
			}
			m = n / 2;
			while (m < js && m > 0) {
				js = js - m;
				m = m / 2;
			}
			js = js + m;
		}
		mmax = 1;
		while (mmax < n) // compute transform
		{
			isp = mmax + mmax;
			ph1 = Math.PI * trans / (double) mmax;
			wxr = Math.cos(ph1);
			wxi = Math.sin(ph1);
			wr = 1.0;
			wi = 0.0;
			for (m = 0; m < mmax; m++) {
				ix = m;
				while (ix + mmax < n) {
					js = ix + mmax;
					tmpr = wr * A[2 * js] - wi * A[2 * js + 1];
					tmpi = wr * A[2 * js + 1] + wi * A[2 * js];
					A[2 * js] = A[2 * ix] - tmpr; // BASIC BUTTERFLY
					A[2 * js + 1] = A[2 * ix + 1] - tmpi;
					A[2 * ix] = A[2 * ix] + tmpr;
					A[2 * ix + 1] = A[2 * ix + 1] + tmpi;
					ix = ix + isp;
				}
				tmpr = wr * wxr - wi * wxi;
				tmpi = wr * wxi + wi * wxr;
				wr = tmpr;
				wi = tmpi;
			}
			mmax = isp;
		}
		if (!analyse) // only divide by n on inverse transform
		{
			for (i = 0; i < n; i++) {
				final int ii = 2 * i;
				A[ii] = A[ii] / (double) n;
				A[ii + 1] = A[ii + 1] / (double) n;
			}
		}
	} // end fft

	/**
	 * Faltung zweier (gleich langer) reeller Zeitreihen
	 * 
	 * @param a
	 *            erste Zeitreihe
	 * @param b
	 *            zweite Zeitreihe
	 * @return Faltung (doppelt so lange wie einzelne Zeitreihen)
	 */
	public static double[] fftconv(double[] a, double[] b) {
		int n = a.length;
		int m = b.length;
		if (n != m) {
			System.out.println("fftconv n!=m");
			System.exit(1);
		}
		double c[] = new double[n];
		double AA[] = new double[4 * n];
		double BB[] = new double[4 * n];
		double CC[] = new double[4 * n];
		for (int i = 0; i < n; i++) {
			AA[2 * i] = a[i];
			BB[2 * i] = b[i];
		}

		// Transformation
		fft(AA);
		fft(BB);

		// Produkt im Frequenzspektrum (komplexe Zahlen)
		for (int i = 0; i < 2 * n; i++) {
			final int ii = 2 * i;
			CC[ii] = AA[ii] * BB[ii] - AA[ii + 1] * BB[ii + 1];
			CC[ii + 1] = AA[ii] * BB[ii + 1] + AA[ii + 1] * BB[ii];
		}

		// Rücktransformation
		ifft(CC);

		// Nur reelle Anteile liefern
		for (int i = 0; i < n; i++) {
			c[i] = CC[2 * i];
		}

		return c;
	} // end fftconv

	public static int fftcrosscorr(double[] a, double[] b) {
		int n = a.length;
		int m = b.length;
		if (n != m) {
			System.out.println("fftconv n!=m");
			System.exit(1);
		}
		double AA[] = new double[4 * n];
		double BB[] = new double[4 * n];
		double CC[] = new double[4 * n];
		for (int i = 0; i < n; i++) {
			AA[2 * i] = a[i];
			BB[2*n - 2 * i] = b[i]; // in der Zeit umdrehen
		}

		// Transformation
		fft(AA);
		fft(BB);

		// Produkt im Frequenzspektrum (komplexe Zahlen)
		for (int i = 0; i < 2 * n; i++) {
			final int ii = 2 * i;
			CC[ii] = AA[ii] * BB[ii] - AA[ii + 1] * BB[ii + 1];
			CC[ii + 1] = AA[ii] * BB[ii + 1] + AA[ii + 1] * BB[ii];
		}

		// Rücktransformation
		ifft(CC);

		// Maximalen reellen Anteile suchen => dort grösste (Pearson) Korrelation
		int offset = 0;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < 2*n; i++) 
			if (CC[2*i] > max) { max = CC[2*i]; offset=i; }

		return offset;
	} 
	
	
	static double[] AA,BB,CC,AAs,BBs,AAs2,BBs2;
	static int offset;
	static double dist;
	
	public static void calcDistance(double[] x,double[] y) {
		// auf Zweierpotenz verlängern
		int rs = x.length;
		int len = Integer.highestOneBit(rs)*2;

		// Größe des temporären Speichers AA/BB/CC ggf. anpassen
		if ((AA==null)||(AA.length!=4*len)) { 
			AA = new double[4 * len];
			BB = new double[4 * len];
			CC = new double[4 * len];
			AAs = new double[len];
			BBs = new double[len];
			AAs2 = new double[len];
			BBs2 = new double[len];
		}
			
			// Zeitreihen AA und BB auf 0 setzen, dann Argumente eintragen
			for (int i = 0; i < 4 * len; i++) {
				AA[i] = 0;
				BB[i] = 0;
			}
			for (int i = 0; i < rs; i++) {
				AA[2 * (len-i-1)] = x[i]; // in der Zeit umdrehen
				BB[2 * i] = y[i]; 
				// Summen berechnen
				if (i==0) AAs[i]=x[0]; else AAs[i] = AAs[i-1]+x[i];
				if (i==0) AAs2[i]=AAs[0]*AAs[0]; else AAs2[i] = AAs2[i-1]+x[i]*x[i];
				if (i==0) BBs[i]=y[0]; else BBs[i] = BBs[i-1]+y[i];
				if (i==0) BBs2[i]=BBs[0]*BBs[0]; else BBs2[i] = BBs2[i-1]+y[i]*y[i];
			}
			for (int i=rs; i<len; ++i) {
				AAs[i] = AAs[rs-1];
				AAs2[i] = AAs2[rs-1];
				BBs[i] = BBs[rs-1];
				BBs2[i] = BBs2[rs-1];
			}
			
			// Transformation
			DFFT.fft(AA);
			DFFT.fft(BB);
	
			// Produkt im Frequenzspektrum (komplexe Zahlen)
			for (int i = 0; i < 2 * len; i++) {
				final int ii = 2 * i;
				CC[ii] = AA[ii] * BB[ii] - AA[ii + 1] * BB[ii + 1];
				CC[ii + 1] = AA[ii] * BB[ii + 1] + AA[ii + 1] * BB[ii];
			}
	
			// Rücktransformation
			DFFT.ifft(CC);

			// Kontrolle: Korrekte Berechnung durch FFT
//			for (int i=3;i<len-3;++i) {
//				int offs = i-(len-1);
//				double c1=0; 
//				if (offs>=0) {
//					for (int ii=0;ii<rs-offs;++ii) c1+=var1.get(ii,s)*var2.get(offs+ii,s); 
//				} else {
//					for (int ii=0;ii<rs+offs;++ii) c1+=var1.get(ii-offs,s)*var2.get(ii,s); 
//				}
//				System.out.println(CC[2*i]+"  "+"  "+c1+"  "+i);
//				assert ( Math.abs(CC[2*i]-c1)<0.0001 );
//			}

			// Maximalen reellen Anteile suchen => dort grösste (Pearson) Korrelation
			int shift = len;
			double max = Double.NEGATIVE_INFINITY;
			final int D = 9;
			for (int i = len - D - 1; i < len + D; i++) {
				int offs = i-(len-1); // aktueller offset
				//System.out.println("i="+i+"   offset="+offs);
				
				final double sumx,sumx2,sumy,sumy2;
				double anz;
				if (offs >= 0) {
					// x:     +-------+
					// y:  +--------+
					sumx = AAs[rs-1-offs];
					sumx2 = AAs2[rs-1-offs];
					if (offs==0) {
						sumy = BBs[rs-1];
						sumy2 = BBs2[rs-1];
					} else {
						sumy = BBs[rs-1]-BBs[offs-1];
						sumy2 = BBs2[rs-1]-BBs2[offs-1];
					}
					anz = rs-offs;

					// direkte Berechnung (debug)
//					double tx=0; for (int ii=0;ii<rs-offs;++ii) tx+=var1.get(ii,s); 
//					double ty=0; for (int ii=0;ii<rs-offs;++ii) ty+=var2.get(offs+ii,s);
//					double tx2=0; for (int ii=0;ii<rs-offs;++ii) tx2+=Math.pow(var1.get(ii,s),2.0); 
//					double ty2=0; for (int ii=0;ii<rs-offs;++ii) ty2+=Math.pow(var2.get(offs+ii,s),2.0);
//					double mx=tx/anz;
//					double my=ty/anz;
//					double sx=0; for (int ii=0;ii<rs-offs;++ii) sx+=Math.pow(var1.get(ii,s)-mx,2.0); sx=Math.sqrt(sx/anz);
//					double sy=0; for (int ii=0;ii<rs-offs;++ii) sy+=Math.pow(var2.get(offs+ii,s)-my,2.0); sy=Math.sqrt(sy/anz);
//					double c1=0; for (int ii=0;ii<rs-offs;++ii) c1+=var1.get(ii,s)*var2.get(offs+ii,s); 
//					double c2=0; for (int ii=0;ii<rs-offs;++ii) c2+=(var1.get(ii,s)-mx)*(var2.get(offs+ii,s)-my); 
//					double fr = 1.0/anz;
//					double cc = (c1 - tx*ty*fr );
//					double dd = Math.sqrt( (tx2 - tx*tx/anz) * (ty2 - ty*ty/anz) );
//					c2/=(anz*sx*sy);
//					System.out.println("  "+tx+" "+ty+" "+mx+" "+my+" "+c1+" "+cc+" "+dd+" "+(cc/dd));
				} else { // offs < 0
					// x:  +-------+
					// y:     +--------+
					sumx = AAs[rs-1]-AAs[-offs-1];
					sumx2 = AAs2[rs-1]-AAs2[-offs-1];
					sumy = BBs[rs-1+offs];
					sumy2 = BBs2[rs-1+offs];
					anz = rs+offs;

					// direkte Berechnung (debug)
//					double tx=0; for (int ii=0;ii<rs+offs;++ii) tx+=var1.get(ii-offs,s); 
//					double ty=0; for (int ii=0;ii<rs+offs;++ii) ty+=var2.get(ii,s);
//					double tx2=0; for (int ii=0;ii<rs+offs;++ii) tx2+=Math.pow(var1.get(ii-offs,s),2.0); 
//					double ty2=0; for (int ii=0;ii<rs+offs;++ii) ty2+=Math.pow(var2.get(ii,s),2.0);
//					double mx=tx/anz;
//					double my=ty/anz;
//					double sx=0; for (int ii=0;ii<rs+offs;++ii) sx+=Math.pow(var1.get(ii-offs,s)-mx,2.0); sx=Math.sqrt(sx/anz);
//					double sy=0; for (int ii=0;ii<rs+offs;++ii) sy+=Math.pow(var2.get(ii,s)-my,2.0); sy=Math.sqrt(sy/anz);
//					double c1=0; for (int ii=0;ii<rs+offs;++ii) c1+=var1.get(ii-offs,s)*var2.get(ii,s); 
//					double c2=0; for (int ii=0;ii<rs+offs;++ii) c2+=(var1.get(ii-offs,s)-mx)*(var2.get(ii,s)-my); 
//					double fr = 1.0/anz;
//					double cc = (c1 - tx*ty*fr );
//					double dd = Math.sqrt( (tx2 - tx*tx/anz) * (ty2 - ty*ty/anz) );
//					c2/=(anz*sx*sy);
//					System.out.println("  "+tx+" "+ty+" "+tx2+" "+ty2+" "+c1+" "+cc+" "+dd+" "+(cc/dd));
				}
				
				double cc = (CC[2*i] - sumx*sumy/anz );
				double dd = Math.sqrt( (sumx2 - sumx*sumx/anz) * (sumy2 - sumy*sumy/anz) );
				//System.out.println("X "+sumx+" "+sumy+" "+sumx2+" "+sumy2+" "+CC[2*i]+" "+cc+" "+dd+" "+(cc/dd));
				cc /= dd;

				if (cc > max) { max = cc; shift=i; }
				//System.out.println("shift="+i+"("+offs+"  d="+cc);

			}
			
			// Offset und Distanz zurückliefern
			offset = shift-(len-1);
			dist = Math.max(0.0,1.0-max);

		
//		System.out.println("offset="+offset+" d="+d);
//
//		// auf Zweierpotenz verlängern
//		int len = Integer.highestOneBit(x.length)*2;
//
////		// Korrekturfaktor für Stddev
////		final double f = 1.0 / Math.sqrt( (double)rs/(double)len );
////final double f=1;
//
//		// Speicher AA/BB/CC ggf. anpassen
//		if ((AA==null)||(AA.length!=4*len)) { 
//			AA = new double[4 * len];
//			BB = new double[4 * len];
//			CC = new double[4 * len];
//			AAs = new double[len];
//			BBs = new double[len];
//			AAs2 = new double[len];
//			BBs2 = new double[len];
//		}
//
//		int rs = x.length;
//		
//			// Zeitreihen AA und BB auf 0 setzen, dann Argumente eintragen
//			for (int i = 0; i < 4 * len; i++) {
//				AA[i] = 0;
//				BB[i] = 0;
//			}
//			for (int i = 0; i < rs; i++) {
//				AA[2 * i] = x[i];
//				BB[2 * (len-i)] = y[i]; // in der Zeit umdrehen
//				if (i==0) AAs[i]=y[0]; else AAs[i] = AAs[i-1]+x[i];
//				if (i==0) AAs2[i]=AAs[0]*AAs[0]; else AAs2[i] = AAs2[i-1]+x[i]*x[i];
//				if (i==0) BBs[i]=y[0]; else BBs[i] = BBs[i-1]+y[i];
//				if (i==0) BBs2[i]=BBs[0]*BBs[0]; else BBs2[i] = BBs2[i-1]+y[i]*y[i];
//			}
//			for (int i=rs; i<len; ++i) {
//				AAs[i] = AAs[rs-1];
//				AAs2[i] = AAs2[rs-1];
//				BBs[i] = BBs[rs-1];
//				BBs2[i] = BBs2[rs-1];
//			}
//			
//			// Transformation
//			DFFT.fft(AA);
//			DFFT.fft(BB);
//	
//			// Produkt im Frequenzspektrum (komplexe Zahlen)
//			for (int i = 0; i < 2 * len; i++) {
//				final int ii = 2 * i;
//				CC[ii] = AA[ii] * BB[ii] - AA[ii + 1] * BB[ii + 1];
//				CC[ii + 1] = AA[ii] * BB[ii + 1] + AA[ii + 1] * BB[ii];
//			}
//	
//			// Rücktransformation
//			DFFT.ifft(CC);
//	
//			// Maximalen reellen Anteile suchen => dort grösste (Pearson) Korrelation
//			int shift = len;
//			int D = Math.min(len,30);
//			double max = Double.NEGATIVE_INFINITY;
////			for (int i = 0; i < 2*len; i++) 
//			for (int i = len - D ; i < len + D ; i++) {
//				final int offs = i-len; // aktueller offset
//				final double sumx,sumx2,sumy,sumy2,f;
//				if (offs >= 0) {
//					// A:  +--------+
//					// B:     +-------+
//					sumx = AAs[len-1]-AAs[offs];
//					sumx2 = AAs2[len-1]-AAs[offs];
//					sumy = BBs[len-1-offs];
//					sumy2 = BBs2[len-1-offs];
//					f = (len-1-offs)/(double)(len);
//				} else { // offs < 0
//					// A:     +--------+
//					// B:  +-------+
//					sumx = AAs[len-1+offs];
//					sumx2 = AAs2[len-1+offs];
//					sumy = BBs[len-1]-BBs[-offs];
//					sumy2 = BBs2[len-1]-BBs2[-offs];
//					f = (len-1+offs)/(double)(len);
//				}
//				double cc = (CC[2*i] - 2.0*sumx*sumy/(double)len + sumx*sumy/(double)(len*len) );
//				double dd = Math.sqrt( (sumx2-2.0*sumx*sumx/(double)len+sumx*sumx/(double)(len*len))
//								     * (sumy2-2.0*sumy*sumy/(double)len+sumy*sumy/(double)(len*len)) );
//				if (dd!=0) {
//					cc /= dd;
//					cc *= f;
//					if (cc > max) { max = cc; shift=i; }
//				}
//			}
//			
//			// Offset und Distanz zurückliefern
//			offset = shift-len;
//			//if (max/(double)len>1.0) System.out.println("MAX "+max); ///(double)len);
//			dist = 1.0-max;
//
//		//System.out.println("offset="+var1);
//		//System.out.println("offset="+var2);
//		//System.out.println("offset="+offset+" d="+dist);
	}
	
	static double getDistValue() { return dist; }
	
	static int getOffset() { return offset; }

} // end class Cxfft
