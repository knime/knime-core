package org.knime.base.node.mine.mds;

public class ClassicalMDS {
	public static void mds(final double[][] data, final double[][] result, final int k) {
		double[][] pm=pivotMatrix(data,k);
		double[] evals=new double[result.length];
		pmds(pm,result,evals);
		for(int i=0; i<result.length; i++) {
			for(int j=0; j<result[0].length; j++) {
				result[i][j]*=Math.sqrt(evals[i]);
			}
		}
	}
	
	private static void pmds(final double[][] input, final double[][] evecs, final double[] evals) {
		int k=input.length;
		int n=input[0].length;
		int d=evecs.length;
		for(int m=0; m<d; m++) {
            evals[m]=normalize(evecs[m]);
        }
		double[][] K=new double[k][k];
		// C^TC berechnen
		selfprod(input, K);
		double[][] temp=new double[d][k];
		// Startvektor der Potenziteration y=Cx
		for(int m=0; m<d; m++) {
			for(int i=0; i<k; i++) {
				for(int j=0; j<n; j++) {
					temp[m][i]+=input[i][j]*evecs[m][j];
				}
			}
		}
		for(int m=0; m<d; m++) {
            evals[m]=normalize(evecs[m]);
        }
		// potenziteration, d top-eigenvektoren von K=C^TC
		final double eps=0.0000001;
		double r=0;
		for(int m=0; m<d; m++) {
            normalize(temp[m]);
        }
		//int iterations=0;
		while(r<1-eps) {
			// NEU
			for(int m=0; m<d; m++) {
                normalize(temp[m]);
            }
			double[][] tempOld=new double[d][k];
			// alte werte merken
			for(int m=0; m<d; m++) {
				for(int i=0; i<k; i++) {
					tempOld[m][i]=temp[m][i];
					temp[m][i]=0;
				}
			}
			// matrix dranmultiplizieren
			for(int m=0; m<d; m++) {
                for(int i=0; i<k; i++) {
                    for(int j=0; j<k; j++)
						temp[m][j]+=K[i][j]*tempOld[m][i];
                }
            }
			// orthogonalisieren
			for(int m=0; m<d; m++) {
				for(int p=0; p<m; p++) {
					double fac=prod(temp[p],temp[m])/prod(temp[p],temp[p]);
					for(int i=0; i<k; i++) {
                        temp[m][i]-=fac*temp[p][i];
                    }
				}
			}
			// normalisieren
			for(int m=0; m<d; m++) {
                evals[m]=normalize(temp[m]);
            }
			r=1;
			for(int m=0; m<d; m++) {
                r=Math.min(Math.abs(prod(temp[m],tempOld[m])),r);
            }
		}
		
		
		// -----------------
		// wahre eigenwerte???
		double[][] tempOld=new double[d][k];
		for(int m=0; m<d; m++) {
            for(int i=0; i<k; i++) {
                for(int j=0; j<k; j++)
					tempOld[m][j]+=K[i][j]*temp[m][i];
            }
        }
		for(int m=0; m<d; m++) {
            evals[m]=normalize(tempOld[m]);
            // -----------------
        }
		
		
		// C^Tx
		for(int m=0; m<d; m++) {
			evals[m]=Math.sqrt(evals[m]);
			for(int i=0; i<n; i++) { // knoten i
				evecs[m][i]=0;
				for(int j=0; j<k; j++) { // pivot j
					evecs[m][i]+=input[j][i]*temp[m][j];
				}
			}
		}
		for(int m=0; m<d; m++) {
            normalize(evecs[m]);
        }
	}
	
	public static double[][] pivotMatrix(final double[][] matrix, final int k) {
		int n=matrix[0].length;
		double[][] result=new double[k][n];
		int pivot=0;
		double[] min=new double[n];
		for(int i=0; i<n; i++) {
            min[i]=Double.MAX_VALUE;
        }
		for(int i=0; i<k; i++) {
			for(int j=0; j<n; j++) {
				result[i][j]=distance2(matrix,pivot,j);
			}
			pivot=0;
			for(int j=0; j<n; j++) {
				min[j]=Math.min(min[j],result[i][j]);
				if(min[j]>min[pivot]) {
                    pivot=j;
                }
			}
		}
		/*
		for(int i=0; i<k; i++)
			for(int j=0; j<n; j++)
				result[i][j]=Math.pow(result[i][j],2);
		*/
		center(result);
		return result;
	}

	
	private static void center(final double[][] distances) {
		int n=distances[0].length;
		int k=distances.length;
		
		// zeilen zentrieren
		for(int j=0; j<k; j++) {
			double avg=0;
			for(int i=0; i<n; i++) {
                avg+=distances[j][i];
            }
			avg/=n;
			for(int i=0; i<n; i++) {
                distances[j][i]-=avg;
            }
		}
		
		// spalten zentrieren
		for(int i=0; i<n; i++) {
			double avg=0;
			for(int j=0; j<k; j++) {
                avg+=distances[j][i];
            }
			avg/=distances.length;
			for(int j=0; j<k; j++) {
                distances[j][i]-=avg;
            }
		}
	}
	
	private static double distance2(final double[][] matrix, final int i, final int j) {
		double result=0;
		for(int m=0; m<matrix.length; m++) {
            result+=Math.pow(matrix[m][i]-matrix[m][j],2);
        }
		return result;
	}
	
	static double distance(final double[][] matrix, final int i, final int j) {
		return Math.sqrt(distance(matrix,i,j));
	}
	
	private static double normalize(final double[] x) {
		double norm=Math.sqrt(prod(x,x));
		for(int i=0; i<x.length; i++) {
            x[i]/=norm;
        }
		return norm;
	}
	
	private static double prod(final double[] x, final double[] y) {
		double result=0;
		for(int i=0; i<x.length; i++) {
            result+=x[i]*y[i];
        }
		return result;
	}
	
	private static void selfprod(final double[][] d, final double[][] result) {
		int k=d.length;
		int n=d[0].length;
		for(int i=0; i<k; i++) {
			for(int j=0; j<=i; j++) {
				double sum=0;
				for(int m=0; m<n; m++) {
                    sum+=d[i][m]*d[j][m];
                }
				result[i][j]=sum;
				result[j][i]=sum;
			}
		}
	}
	
	public static void randomize(final double[][] matrix) {
		java.util.Random random=new java.util.Random(1L);
		for(int i=0; i<matrix.length; i++) {
			for(int j=0; j<matrix[0].length; j++) {
				matrix[i][j]=random.nextDouble();
			}
		}
	}
    
}
