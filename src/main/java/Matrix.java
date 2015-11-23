import java.util.*;

public class Matrix {

	protected int[][] data;

	public Matrix(int rowSize, int colSize) {
		this(new int[rowSize][colSize], true);
	}

	public Matrix(int[][] data) {
		this(data, false);
	}

	protected Matrix(int[][] data, boolean skipValidation) {
		if (!skipValidation && !isValidMatrixData(data)) {
			throw new IllegalArgumentException("invalid matrix data");
		}
		this.data = data;
	}

	public int rowSize() {
		return data.length;
	}

	public int columnSize() {
		return data.length == 0 ? 0 : data[0].length;
	}

	public int get(int rowIndex, int columnIndex) {
		if (!exists(rowIndex, columnIndex)) {
			throw new IndexOutOfBoundsException("position (" + rowIndex + ", " + columnIndex + ") out of bounds");
		}
		return data[rowIndex][columnIndex];
	}

	public RowIterator rowIterator(int rowIndex) {
		return new RowIterator(rowIndex);
	}

	public ColumnIterator columnIterator(int columnIndex) {
		return new ColumnIterator(columnIndex);
	}

	public boolean exists(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || !(rowIndex < data.length)) return false;
		if (columnIndex < 0 || !(columnIndex < data[rowIndex].length)) return false;
		return true;
	}

	public Matrix multiply(Matrix bMatrix) {
		return multiply(this, bMatrix);
	}

	public Matrix add(Matrix bMatrix) {
		return add(this, bMatrix);
	}

	public Matrix addToSelf(Matrix matrix) {
		return add(this, matrix, this);
	}

	public MatrixPartition partition(int maxBlockRowSize, int maxBlockColumnSize) {
		int originRowSize = this.rowSize();
		int originColSize = this.columnSize();

		int resultantRowSize = (int) (Math.ceil(originRowSize / (double) maxBlockRowSize));
		int resultantColSize = (int) (Math.ceil(originColSize / (double) maxBlockColumnSize));
		// System.out.printf("resultant = (%d x %d) => (%d x %d) \n\n", resultantRowSize, resultantColSize, maxBlockRowSize, maxBlockColumnSize);

		Matrix[][] partitionData = new Matrix[resultantRowSize][resultantColSize];

		for (int i = 0; i < resultantRowSize; i++) {
			for (int j = 0; j < resultantColSize; j++) {

				int rowOffset = i * maxBlockRowSize;
				int colOffset = j * maxBlockColumnSize;

				// if it is last row or column, calculate size by deducting the total size with current offset
				int actualRowSize =
						(i + 1 < resultantRowSize) ?
								maxBlockRowSize :
								originRowSize - rowOffset;
				int actualColSize =
						(j + 1 < resultantColSize) ?
								maxBlockColumnSize :
								originColSize - colOffset;

				int[][] matrixData = new int[actualRowSize][actualColSize];

				for (int h = 0; h < actualRowSize; h++) {
					for (int k = 0; k < actualColSize; k++) {
						int row = rowOffset + h;
						int col = colOffset + k;
						int item = data[row][col];
						matrixData[h][k] = item;
					}
				}

				partitionData[i][j] = new Matrix(matrixData);

			}
		}
		return new MatrixPartition(partitionData, true);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		int rowSize = rowSize();
		int colSize = columnSize();

		buf
			.append("Matrix ")
			.append(rowSize)
			.append(" x ")
			.append(colSize)
			.append(" : [")
			.append("\n");

		boolean firstRow = true;

		for (int row = 0; row < rowSize; row++) {

			if (!firstRow) buf.append("\n");
			else firstRow = false;

			boolean firstCol = true;

			buf.append("  ");

			for (int col = 0; col < colSize; col++) {

				if (!firstCol) buf.append(", ");
				else firstCol = false;

				int item = data[row][col];
				buf.append(item);
			}
		}

		buf
			.append("\n")
			.append("]");

		return buf.toString();
	}

	public String serialize() {
		int rowSize = rowSize();
		int columnSize = columnSize();
		StringBuilder buf = new StringBuilder();

		buf.append(rowSize).append(" ").append(columnSize).append("\n");

		for (int i = 0; i < rowSize; i++) {
			for (int j = 0; j < columnSize; j++) {
				buf.append(data[i][j]).append(" ");
			}
			buf.append("\n");
		}

		return buf.toString();
	}

	public static Matrix deserialize(String data) {
		try {
			Scanner sc = new Scanner(data);
			int rowSize = sc.nextInt();
			int colSize = sc.nextInt();

			int[][] matrixData = new int[rowSize][colSize];
			for (int i = 0; i < rowSize; i++) {
				for (int j = 0; j < colSize; j++) {
					matrixData[i][j] = sc.nextInt();
				}
			}

			return new Matrix(matrixData);
		} catch (RuntimeException ex) {
			throw new IllegalArgumentException("invalid data to deserialize");
		}
	}

	public static Matrix multiply(Matrix aMatrix, Matrix bMatrix) {
		if (aMatrix.columnSize() != bMatrix.rowSize()) {
			throw new UnsupportedOperationException("cannot multiply matrices with first matrix's column size (" + aMatrix.columnSize() + ") unequal to second matrix's row (" + bMatrix.rowSize() + ") size");
		}
		int resultantRowSize = aMatrix.rowSize();
		int resultantColSize = bMatrix.columnSize();
		int[][] newMatrixData = new int[resultantRowSize][resultantColSize];
		for (int row = 0; row < resultantRowSize; row++) {
			for (int col = 0; col < resultantColSize; col++) {
				VectorIterator rowVector = aMatrix.rowIterator(row);
				VectorIterator colVector = bMatrix.columnIterator(col);
				newMatrixData[row][col] = vectorDotProduct(rowVector, colVector);
			}
		}
		return new Matrix(newMatrixData, true);
	}

	public static Matrix add(Matrix aMatrix, Matrix bMatrix) {
		int resultantRowSize = aMatrix.rowSize();
		int resultantColSize = aMatrix.columnSize();
		return add(aMatrix, bMatrix, new Matrix(resultantRowSize, resultantColSize));
	}

	private static Matrix add(Matrix aMatrix, Matrix bMatrix, Matrix targetMatrix) {
		if (aMatrix.rowSize() != bMatrix.rowSize() || aMatrix.columnSize() != bMatrix.columnSize()) {
			throw new UnsupportedOperationException("cannot add matrices A(" + aMatrix.rowSize() + "x" + aMatrix.columnSize() + ") to B(" + bMatrix.rowSize() + "x" + bMatrix.columnSize() + ") with different sizes");
		}
		int resultantRowSize = aMatrix.rowSize();
		int resultantColSize = aMatrix.columnSize();
		for (int row = 0; row < resultantRowSize; row++) {
			for (int col = 0; col < resultantColSize; col++) {
				int aItem = aMatrix.data[row][col];
				int bItem = bMatrix.data[row][col];
				int sum = (aItem + bItem);

				targetMatrix.data[row][col] = sum;
			}
		}
		return targetMatrix;
	}

	public static boolean isValidMatrixData(int[][] data) {
		if (data == null) return false;
		int columnCount = -1;
		for (int[] column : data) {
			if (column == null) return false;
			if (columnCount < 0) columnCount = column.length;
			if (columnCount != column.length) return false;
		}
		return true;
	}

	public static int vectorDotProduct(VectorIterator aVector, VectorIterator bVector) {
		if (aVector.size() != bVector.size()) {
				throw new UnsupportedOperationException("cannot compute dot product of vectors with different sizes");
		}
		int sum = 0;
		while (aVector.hasNext() && bVector.hasNext()) {
			int a = aVector.next();
			int b = bVector.next();
			sum += (a * b);
		}
		return sum;
	}

	public interface VectorIterator extends Iterator<Integer>, Iterable<Integer> {
		public int size();
	}

	public class RowIterator implements VectorIterator {
		private int rowIndex;
		private int colIndex;
		private RowIterator(int rowIndex) {
			if (!exists(rowIndex, 0)) throw new IndexOutOfBoundsException("row index " + rowIndex + " out of bounds");
			this.rowIndex = rowIndex;
			this.colIndex = -1;
		}
		@Override
		public boolean hasNext() {
			return exists(rowIndex, colIndex + 1);
		}
		@Override
		public Integer next() {
			if (!hasNext()) throw new NoSuchElementException();
			colIndex++;
			return data[rowIndex][colIndex];
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Iterator<Integer> iterator() {
			return this;
		}
		@Override
		public int size() {
			return data[rowIndex].length;
		}
	}

	public class ColumnIterator implements VectorIterator {
		private int rowIndex;
		private int colIndex;
		private ColumnIterator(int colIndex) {
			if (!exists(0, colIndex)) throw new IndexOutOfBoundsException("column index " + colIndex + " out of bounds");
			this.rowIndex = -1;
			this.colIndex = colIndex;
		}
		@Override
		public boolean hasNext() {
			return exists(rowIndex + 1, colIndex);
		}
		@Override
		public Integer next() {
			if (!hasNext()) throw new NoSuchElementException();
			rowIndex++;
			return data[rowIndex][colIndex];
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Iterator<Integer> iterator() {
			return this;
		}
		@Override
		public int size() {
			return data.length;
		}
	}

	public static Matrix generateMatrix(int rowSize, int colSize, int min, int max) {
		int[][] data = new int[rowSize][colSize];
		for (int i = 0; i < rowSize; i++) {
			for (int j = 0; j < colSize; j++) {
				data[i][j] = (int) (Math.random() * (max - min) + min);
			}
		}
		return new Matrix(data, true);
	}

	public static void main(String[] args) {
		Matrix m1 = generateMatrix(100, 200, 10, 20);
		Matrix m2 = generateMatrix(200, 100 , 10, 20);
		MatrixPartition mp1 = m1.partition(4, 4);
		MatrixPartition mp2 = m2.partition(4, 4);
		MatrixPartition result = mp1.multiply(mp2);
		System.out.println(result);
		System.out.println(result.toMatrix());
		System.out.println("ok");
	}

}