import java.util.*;

public class MatrixPartition {

	private Matrix[][] data;

	public MatrixPartition(Matrix[][] data) {
		this(data, false);
	}

	protected MatrixPartition(Matrix[][] data, boolean skipValidation) {
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

	public MatrixPartition multiply(MatrixPartition bMatrix) {
		return multiply(this, bMatrix);
	}

	public Matrix toMatrix() {
		int partitionRowSize = this.rowSize();
		int partitionColSize = this.columnSize();
		int resultantRowSize = 0;
		int resultantColSize = 0;

		// We can compute size as this because all matrices are assumed block matrices
		for (int i = 0; i < partitionRowSize; i++) resultantRowSize += data[i][0].rowSize();
		for (int j = 0; j < partitionColSize; j++) resultantColSize += data[0][j].columnSize();

		int[][] matrixData = new int[resultantRowSize][resultantColSize];

		int rowOffset = 0;
		int colOffset = 0;

		for (int i = 0; i < partitionRowSize; i++) {
			// We can safely assume the row sizes are all the same within all rows because they are all block matrices
			int lastMatrixRowSize = 0;
			for (int j = 0; j < partitionColSize; j++) {

				Matrix matrix = data[i][j];
				int matrixRowSize = matrix.rowSize();
				int matrixColSize = matrix.columnSize();

				for (int m = 0; m < matrixRowSize; m++) {
					for (int n = 0; n < matrixColSize; n++) {
						int item = matrix.data[m][n];
						int row = rowOffset + m;
						int col = colOffset + n;
						matrixData[row][col] = item;
					}
				}

				colOffset += matrixColSize;
				lastMatrixRowSize = matrixRowSize;

			}
			rowOffset += lastMatrixRowSize;
			colOffset = 0;
		}
		return new Matrix(matrixData, true);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		int rowSize = rowSize();
		int colSize = columnSize();

		buf
				.append("MatrixPartition ")
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

				buf.append("M(").append(row).append(",").append(col).append(")");
			}
		}

		buf
				.append("\n")
				.append("]")
				.append("\n");

		for (int row = 0; row < rowSize; row++) {
			for (int col = 0; col < colSize; col++) {
				Matrix item = data[row][col];
				buf
						.append("\n")
						.append("M(")
						.append(row)
						.append(",")
						.append(col)
						.append("):")
						.append('\n')
						.append(item.toString())
						.append('\n');
			}
		}

		return buf.toString();
	}

	public static MatrixPartition multiply(MatrixPartition aMatrix, MatrixPartition bMatrix) {
		if (aMatrix.columnSize() != bMatrix.rowSize()) {
			throw new UnsupportedOperationException("cannot multiply matrices with first matrix's column size (" + aMatrix.columnSize() + ") unequal to second matrix's row (" + bMatrix.rowSize() + ") size");
		}
		int resultantRowSize = aMatrix.rowSize();
		int resultantColSize = bMatrix.columnSize();
		Matrix[][] newMatrixData = new Matrix[resultantRowSize][resultantColSize];
		for (int row = 0; row < resultantRowSize; row++) {
			for (int col = 0; col < resultantColSize; col++) {
				VectorIterator rowVector = aMatrix.rowIterator(row);
				VectorIterator colVector = bMatrix.columnIterator(col);
				newMatrixData[row][col] = vectorDotProduct(rowVector, colVector);
			}
		}
		return new MatrixPartition(newMatrixData, true);
	}

	public static boolean isValidMatrixData(Matrix[][] data) {
		if (data == null) return false;
		int columnCount = -1;
		for (Matrix[] column : data) {
			if (column == null) return false;
			if (columnCount < 0) columnCount = column.length;
			if (columnCount != column.length) return false;
		}
		return true;
	}

	public static Matrix vectorDotProduct(VectorIterator aVector, VectorIterator bVector) {
		if (aVector.size() != bVector.size()) {
			throw new UnsupportedOperationException("cannot compute dot product of vectors with different sizes");
		}
		Matrix sum = null;
		while (aVector.hasNext() && bVector.hasNext()) {
			Matrix a = aVector.next();
			Matrix b = bVector.next();
			Matrix product = a.multiply(b);
			if (sum == null) {
				sum = new Matrix(product.rowSize(), product.columnSize());
			}
			sum.addToSelf(product);
		}
		return sum;
	}

	public interface VectorIterator extends Iterator<Matrix>, Iterable<Matrix> {
		public int size();
	}

	public class RowIterator implements VectorIterator {
		private int rowIndex;
		private int colIndex;
		private RowIterator(int rowIndex) {
			if (!exists(rowIndex, 0)) throw new IllegalArgumentException("row index " + rowIndex + " out of bounds");
			this.rowIndex = rowIndex;
			this.colIndex = -1;
		}
		@Override
		public boolean hasNext() {
			return exists(rowIndex, colIndex + 1);
		}
		@Override
		public Matrix next() {
			if (!hasNext()) throw new NoSuchElementException();
			colIndex++;
			return data[rowIndex][colIndex];
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Iterator<Matrix> iterator() {
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
			if (!exists(0, colIndex)) throw new IllegalArgumentException("column index " + colIndex + " out of bounds");
			this.rowIndex = -1;
			this.colIndex = colIndex;
		}
		@Override
		public boolean hasNext() {
			return exists(rowIndex + 1, colIndex);
		}
		@Override
		public Matrix next() {
			if (!hasNext()) throw new NoSuchElementException();
			rowIndex++;
			return data[rowIndex][colIndex];
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Iterator<Matrix> iterator() {
			return this;
		}
		@Override
		public int size() {
			return data.length;
		}
	}

}