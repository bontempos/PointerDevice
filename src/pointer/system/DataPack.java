/* 
 *  TODO
 *  an standard object for all submiting packages
 *  FORMAT:  [byte position 0] COMMAND    [1 to 3] PACK_SIZE     [4 ... ] DATA
 */

package pointer.system;

import java.io.ByteArrayOutputStream ;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DataPack implements Serializable {
	private static ByteBuffer buffer;
	private static final int PARAM_POSITION = 0;
	private static final int PARAM_LENGTH = 1;
	private static final int PACK_SIZE_POSITION = 1;
	private static final int PACK_SIZE_LENGTH = 3;

}
