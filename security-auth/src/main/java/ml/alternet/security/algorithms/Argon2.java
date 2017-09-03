package ml.alternet.security.algorithms;

import static ml.alternet.security.algorithms.Argon2.Constants.ARGON2_BLOCK_SIZE;
import static ml.alternet.security.algorithms.Argon2.Constants.ARGON2_PREHASH_DIGEST_LENGTH;
import static ml.alternet.security.algorithms.Argon2.Constants.ARGON2_PREHASH_SEED_LENGTH;
import static ml.alternet.security.algorithms.Argon2.Constants.ARGON2_QWORDS_IN_BLOCK;
import static ml.alternet.security.algorithms.Argon2.Constants.ARGON2_SYNC_POINTS;
import static ml.alternet.security.algorithms.Argon2.Constants.Defaults.LANES_DEF;
import static ml.alternet.security.algorithms.Argon2.Constants.Defaults.LOG_M_COST_DEF;
import static ml.alternet.security.algorithms.Argon2.Constants.Defaults.OUTLEN_DEF;
import static ml.alternet.security.algorithms.Argon2.Constants.Defaults.TYPE_DEF;
import static ml.alternet.security.algorithms.Argon2.Constants.Defaults.T_COST_DEF;
import static ml.alternet.security.algorithms.Argon2.Constants.Defaults.VERSION_DEF;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ml.alternet.security.algorithms.Argon2.Constants.Constraints;
import ml.alternet.security.auth.hashers.impl.Argon2Hasher.Argon2Bridge;
import ml.alternet.security.auth.hashers.impl.Argon2Hasher.Argon2Bridge.Type;
import ml.alternet.security.binary.LittleEndian;
import ml.alternet.util.StringUtil;

public class Argon2 {

    private byte[] output;
    private int outputLength; // -l N
    private double duration;

    private byte[] password;
    private byte[] salt;
    private byte[] secret;
    private byte[] additional;

    private int iterations; // -t N
    private int memory; // -m N
    private int lanes; // -p N
    private int threads; // -p N

    private int version; // -v (10/13)
    private Type type;

    private boolean clearMemory = true;

    public Argon2() {
        this.lanes = LANES_DEF;
        this.outputLength = OUTLEN_DEF;
        this.threads = LANES_DEF;
        this.memory = 1 << LOG_M_COST_DEF;
        this.iterations = T_COST_DEF;
        this.version = VERSION_DEF;
        this.type = TYPE_DEF;
    }

    public byte[] hash(byte[] password, byte[] salt){
        this.password = password;
        this.salt = salt;
        Constraints.validateInput(this);
        long start = System.nanoTime();
        Instance instance = new Instance(this);
        instance.initialize(this);
        instance.fillMemoryBlocks();
        instance.finalize(this);
        duration = (System.nanoTime() - start) / 1000000000.0;
        return this.output;
    }

    public void clearPassword() {
        if (password != null && isClearMemory()) {
            Arrays.fill(password, 0, password.length, (byte) 0);
            password = null;
        }
    }

    public void clearSecret() {
        if (secret != null && isClearMemory()) {
            Arrays.fill(secret, 0, secret.length, (byte) 0);
            secret = null;
        }
    }

    public void clearAdditional() {
        if (additional != null && isClearMemory()) {
            Arrays.fill(additional, 0, additional.length, (byte) 0);
            additional = null;
        }
    }

    public void printSummary(){
        System.out.println("Type:\t\t" + type);
        System.out.println("Iterations:\t" + iterations);
        System.out.println("Memory:\t\t" + memory + " KiB");
        System.out.println("Parallelism:\t" + lanes);
        System.out.println("Hash:\t\t" + StringUtil.getHex(this.output, false));
        System.out.println(duration + " seconds");
    }

    public Argon2 setMemory(int memory){
        this.memory = 1 << memory;
        return this;
    }

    public Argon2 setMemoryInKiB(int memory) {
        this.memory = memory;
        return this;
    }

    public Argon2 setIterations(int iterations){
        this.iterations = iterations;
        return this;
    }

    public Argon2 setParallelism(int parallelism){
        this.threads = parallelism;
        this.lanes = parallelism;
        return this;
    }

    public Argon2 setType(Type type){
        this.type = type;
        return this;
    }

    public Argon2 setVersion(int version){
        this.version = version;
        return this;
    }

    public Argon2 setOutputLength(int outputLength) {
        this.outputLength = outputLength;
        return this;
    }

    public void setOutput(byte[] finalResult) {
        this.output = finalResult;
    }

    public Argon2 setSecret(byte[] secret){
        this.secret = secret;
        return this;
    }

    public Argon2 setAdditional(byte[] additional){
        this.additional = additional;
        return this;
    }

    public void setClearMemory(boolean clearMemory) {
        this.clearMemory = clearMemory;
    }

    public int getOutputLength() {
        return outputLength;
    }

    public byte[] getPassword() {
        return password;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getSecret() {
        return secret;
    }

    public byte[] getAdditional() {
        return additional;
    }

    public int getIterations() {
        return iterations;
    }

    public int getMemory() {
        return memory;
    }

    public int getLanes() {
        return lanes;
    }

    public int getThreads() {
        return threads;
    }

    public int getVersion() {
        return version;
    }

    public Type getType() {
        return type;
    }

    public boolean isClearMemory() {
        return clearMemory;
    }

    public static class Instance {

        public Block[] memory;
        private int version;
        private int passes;
        private int segmentLength;
        private int laneLength;
        private int lanes;
        private int threads;

        private Argon2Bridge.Type type;

        public Instance(Argon2 argon2) {
            this.version = argon2.getVersion();
            this.passes = argon2.getIterations();
            this.lanes = argon2.getLanes();
            this.threads = argon2.getThreads();
            this.type = argon2.getType();

            initMemory(argon2);
        }

        private void initMemory(Argon2 argon2) {
            /* 2. Align memory size */
            /* Minimum memoryBlocks = 8L blocks, where L is the number of lanes */
            int memoryBlocks = argon2.getMemory();

            if (memoryBlocks < 2 * ARGON2_SYNC_POINTS * argon2.getLanes()) {
                memoryBlocks = 2 * ARGON2_SYNC_POINTS * argon2.getLanes();
            }

            this.segmentLength = memoryBlocks / (argon2.getLanes() * ARGON2_SYNC_POINTS);
            this.laneLength = segmentLength * ARGON2_SYNC_POINTS;
            /* Ensure that all segments have equal length */
            memoryBlocks = segmentLength * (argon2.getLanes() * ARGON2_SYNC_POINTS);

            this.memory = new Block[memoryBlocks];

            for(int i=0;i<memory.length;i++){
                memory[i] = new Block();
            }
        }

        public void initialize(Argon2 argon2){
            byte[] initialHash = Functions.initialHash(argon2);
            fillFirstBlocks(initialHash);
        }

        private void fillFirstBlocks(byte[] initialHash) {

            byte[] blockhash_bytes;

            for (int i=0;i<this.getLanes();i++) {

                byte[] iBytes = LittleEndian.intToBytes(i);
                byte[] zeroBytes = LittleEndian.intToBytes(0);
                byte[] oneBytes = LittleEndian.intToBytes(1);

                System.arraycopy(zeroBytes, 0, initialHash, ARGON2_PREHASH_DIGEST_LENGTH, 4);
                System.arraycopy(iBytes, 0, initialHash, ARGON2_PREHASH_DIGEST_LENGTH + 4, 4);
                blockhash_bytes = Functions.blake2bLong(initialHash, ARGON2_BLOCK_SIZE);
                int index = i * this.getLaneLength() + 0;
                this.memory[index].fromBytes(blockhash_bytes);

                System.arraycopy(oneBytes, 0, initialHash, ARGON2_PREHASH_DIGEST_LENGTH, 4);
                blockhash_bytes = Functions.blake2bLong(initialHash, ARGON2_BLOCK_SIZE);
                this.memory[i * this.getLaneLength() + 1].fromBytes(blockhash_bytes);
            }
        }

        public void fillMemoryBlocks() {
            Fillers.fillMemoryBlocks(this);
        }

        public void finalize(Argon2 argon2) {

            if (argon2 != null) {

                Block blockhash = new Block();
                blockhash.copyBlock(this.memory[this.getLaneLength() - 1]);

                /* XOR the last blocks */
                for (int i=1; i<this.getLanes(); ++i) {
                    int last_block_in_lane = i * this.getLaneLength() + (this.getLaneLength() - 1);
                    blockhash.xorBlock(this.memory[last_block_in_lane]);
                }

                /* Hash the result */
                byte[] blockhash_bytes = blockhash.toBytes();
                byte[] finalResult = Functions.blake2bLong(blockhash_bytes,
                        argon2.getOutputLength());

                argon2.setOutput(finalResult);

                if(argon2.isClearMemory()) {
                    this.clear();
                }
            }
        }

        public void clear() {
            for (Block b: memory){
                b.clear();
            }
            memory = null;
        }

        public Block[] getMemory() {
            return memory;
        }

        public int getVersion() {
            return version;
        }

        public int getPasses() {
            return passes;
        }

        public int getSegmentLength() {
            return segmentLength;
        }

        public int getLaneLength() {
            return laneLength;
        }

        public int getLanes() {
            return lanes;
        }

        public int getThreads() {
            return threads;
        }

        public Type getType() {
            return type;
        }
    }

    public static class Position {

        public int pass;
        public int lane;
        public byte slice;
        public int index;

        public Position(int pass, int lane, byte slice, int index) {
            this.pass = pass;
            this.lane = lane;
            this.slice = slice;
            this.index = index;
        }
    }

    public static class Block {

        /* 128 * 8 Byte QWords */
        public long[] v;

        public Block() {
            v = new long[ARGON2_QWORDS_IN_BLOCK];
        }

        public void fromBytes(byte[] input){
            assert(input.length == ARGON2_BLOCK_SIZE);

            for(int i=0;i<v.length;i++){
                byte[] slice = Arrays.copyOfRange(input,i*8, (i+1)*8);
                v[i] = LittleEndian.bytesToLong(slice);
            }
        }

        public byte[] toBytes(){
            byte[] result = new byte[ARGON2_BLOCK_SIZE];

            for(int i=0;i<v.length;i++){
                byte[] bytes = LittleEndian.longToBytes(v[i]);
                System.arraycopy(bytes, 0, result, i*bytes.length, bytes.length);
            }

            return result;
        }

        public void copyBlock(Block other){
            System.arraycopy(other.v, 0, v, 0, v.length);
        }

        public void xorBlock(Block other){
            for(int i = 0; i< v.length; i++){
                v[i] = v[i] ^ other.v[i];
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (long value : v) {
                result.append(StringUtil.getHex((LittleEndian.longToBytes(value)), false));
            }

            return result.toString();
        }

        void clear() {
            Arrays.fill(v, 0);
        }

    }

    public void finalize(Instance instance, Argon2 argon2) {

        if (argon2 != null && instance != null) {

            Block blockhash = new Block();
            blockhash.copyBlock(instance.memory[instance.getLaneLength() - 1]);

            /* XOR the last blocks */
            for (int i=1; i<instance.getLanes(); ++i) {
                int last_block_in_lane = i * instance.getLaneLength() + (instance.getLaneLength() - 1);
                blockhash.xorBlock(instance.memory[last_block_in_lane]);
            }

            /* Hash the result */
            byte[] blockhash_bytes = blockhash.toBytes();
            byte[] finalResult = Functions.blake2bLong(blockhash_bytes,
                    argon2.getOutputLength());

            argon2.setOutput(finalResult);

            if(argon2.isClearMemory()) {
                instance.clear();
            }
        }
    }

    static class Fillers {

        static void fillBlock(Block prevBlock, Block refBlock, Block nextBlock, boolean withXor) {

            Block blockR = new Block();
            Block blockTmp = new Block();

            blockR.copyBlock(refBlock);
            blockR.xorBlock(prevBlock);
            blockTmp.copyBlock(blockR);

            if (withXor) {
                blockTmp.xorBlock(nextBlock);
            }

            /* Apply Blake2 on columns of 64-bit words: (0,1,...,15) , then
            (16,17,..31)... finally (112,113,...127) */
            for (int i = 0; i < 8; ++i) {

                Functions.roundFunction(blockR,
                        16 * i, 16 * i + 1, 16 * i + 2,
                        16 * i + 3, 16 * i + 4, 16 * i + 5,
                        16 * i + 6, 16 * i + 7, 16 * i + 8,
                        16 * i + 9, 16 * i + 10, 16 * i + 11,
                        16 * i + 12, 16 * i + 13, 16 * i + 14,
                        16 * i + 15
                        );
            }

            /* Apply Blake2 on rows of 64-bit words: (0,1,16,17,...112,113), then
            (2,3,18,19,...,114,115).. finally (14,15,30,31,...,126,127) */
            for (int i = 0; i < 8; i++) {

                Functions.roundFunction(blockR,
                        2 * i, 2 * i + 1, 2 * i + 16,
                        2 * i + 17, 2 * i + 32, 2 * i + 33,
                        2 * i + 48, 2 * i + 49, 2 * i + 64,
                        2 * i + 65, 2 * i + 80, 2 * i + 81,
                        2 * i + 96, 2 * i + 97, 2 * i + 112,
                        2 * i + 113
                        );

                nextBlock.copyBlock(blockTmp);
                nextBlock.xorBlock(blockR);
            }
        }

        public static void fillMemoryBlocks(Instance instance) {

            if (instance.getThreads() == 1) {
                fillMemoryBlockSingleThreaded(instance);
            } else {
                fillMemoryBlockMultiThreaded(instance);
            }
        }

        private static void fillMemoryBlockSingleThreaded(Instance instance) {

            for (int i = 0; i < instance.getPasses(); i++) {
                for (int j = 0; j < ARGON2_SYNC_POINTS; j++) {
                    for (int k = 0; k < instance.getLanes(); k++) {
                        Position position = new Position(i, k, (byte) j, 0);
                        Fillers.fillSegment(instance, position);
                    }
                }
            }
        }

        private static void fillMemoryBlockMultiThreaded(Instance instance) {

            Thread[] threads = new Thread[instance.getLanes()];

            for (int i = 0; i < instance.getPasses(); i++) {
                for (int j = 0; j < ARGON2_SYNC_POINTS; j++) {

                    /* 2. Calling threads */
                    for (int k = 0; k < instance.getLanes(); k++) {

                        /* 2.1 Join a thread if limit is exceeded */
                        if (k >= instance.getThreads()) {
                            try {
                                threads[k - instance.getThreads()].join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        Position position = new Position(i, k, (byte) j, 0);

                        /* 2.2 Create thread */
                        threads[k] = new Thread(() -> Fillers.fillSegment(instance, position));
                        threads[k].start();
                    }

                    /* 3. Joining remaining threads */
                    for (int k = instance.getLanes() - instance.getThreads(); k < instance.getLanes(); k++) {
                        try {
                            threads[k].join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        static void fillSegment(Instance instance, Position position) {
            Block refBlock, currentBlock;
            Block addressBlock = null, inputBlock = null, zeroBlock = null;
            long pseudoRandom;
            int refIndex, refLane;
            int prevOffset, currentOffset;
            int startingIndex;
            boolean data_independent_addressing;

            if (instance == null) {
                return;
            }

            data_independent_addressing =
                    (instance.getType() == Type.argon2i) ||
                    (instance.getType() == Type.argon2id && (position.pass == 0) &&
                    (position.slice < Constants.ARGON2_SYNC_POINTS / 2));

            if (data_independent_addressing) {
                addressBlock = new Block();
                zeroBlock = new Block();
                inputBlock = new Block();

                inputBlock.v[0] = position.pass;
                inputBlock.v[1] = position.lane;
                inputBlock.v[2] = position.slice;
                inputBlock.v[3] = instance.memory.length;
                inputBlock.v[4] = instance.getPasses();
                inputBlock.v[5] = instance.getType().ordinal();
            }

            startingIndex = 0;

            if ((0 == position.pass) && (0 == position.slice)) {
                startingIndex = 2; /* we have already generated the first two blocks */

                /* Don't forget to generate the first block of addresses: */
                if (data_independent_addressing) {
                    nextAddresses(addressBlock, inputBlock, zeroBlock);
                }
            }

            /* Offset of the current block */
            currentOffset = position.lane * instance.getLaneLength() +
                    position.slice * instance.getSegmentLength() + startingIndex;

            if (0 == currentOffset % instance.getLaneLength()) {
                /* Last block in this lane */
                prevOffset = currentOffset + instance.getLaneLength() - 1;
            } else {
                /* Previous block */
                prevOffset = currentOffset - 1;
            }

            for (int i = startingIndex; i < instance.getSegmentLength();
                    ++i, ++currentOffset, ++prevOffset) {
                /*1.1 Rotating prev_offset if needed */
                if (currentOffset % instance.getLaneLength() == 1) {
                    prevOffset = currentOffset - 1;
                }


                /* 1.2 Computing the index of the reference block */
                /* 1.2.1 Taking pseudo-random value from the previous block */
                if (data_independent_addressing) {
                    if (i % Constants.ARGON2_ADDRESSES_IN_BLOCK == 0) {
                        nextAddresses(addressBlock, inputBlock, zeroBlock);
                    }
                    pseudoRandom = addressBlock.v[i % Constants.ARGON2_ADDRESSES_IN_BLOCK];
                } else {
                    pseudoRandom = instance.memory[prevOffset].v[0];
                }

                /* 1.2.2 Computing the lane of the reference block */
                refLane = (int) (((pseudoRandom >>> 32)) % instance.getLanes());


                if ((position.pass == 0) && (position.slice == 0)) {
                    /* Can not reference other lanes yet */
                    refLane = position.lane;
                }

                /* 1.2.3 Computing the number of possible reference block within the
                 * lane.
                 */

                position.index = i;
                refIndex = indexAlpha(instance, position, pseudoRandom,
                        refLane == position.lane);


                /* 2 Creating a new block */
                refBlock = instance.memory[(instance.getLaneLength()) * refLane + refIndex];
                currentBlock = instance.memory[currentOffset];

                if (Constants.ARGON2_VERSION_10 == instance.getVersion()) {
                    /* version 1.2.1 and earlier: overwrite, not XOR */
                    Fillers.fillBlock(instance.memory[prevOffset], refBlock, currentBlock, false);
                } else {
                    if (0 == position.pass) {
                        Fillers.fillBlock(instance.memory[prevOffset], refBlock,
                                currentBlock, false);
                    } else {
                        Fillers.fillBlock(instance.memory[prevOffset], refBlock,
                                currentBlock, true);
                    }
                }
            }
        }

        private static void nextAddresses(Block address_block, Block input_block, Block zero_block) {
            input_block.v[6]++;
            Fillers.fillBlock(zero_block, input_block, address_block, false);
            Fillers.fillBlock(zero_block, address_block, address_block, false);
        }

        private static int indexAlpha(Instance instance, Position position, long pseudoRand,
                boolean same_lane) {
            /*
             * Pass 0:
             *      This lane : all already finished segments plus already constructed
             * blocks in this segment
             *      Other lanes : all already finished segments
             * Pass 1+:
             *      This lane : (SYNC_POINTS - 1) last segments plus already constructed
             * blocks in this segment
             *      Other lanes : (SYNC_POINTS - 1) last segments
             */
            int reference_area_size;
            long relative_position;
            int start_position, absolute_position;

            if (0 == position.pass) {
                /* First pass */
                if (0 == position.slice) {
                    /* First slice */
                    reference_area_size =
                            position.index - 1; /* all but the previous */
                } else {
                    if (same_lane) {
                        /* The same lane => add current segment */
                        reference_area_size =
                                position.slice * instance.getSegmentLength() +
                                position.index - 1;
                    } else {
                        reference_area_size =
                                position.slice * instance.getSegmentLength() +
                                ((position.index == 0) ? (-1) : 0);
                    }
                }
            } else {
                /* Second pass */
                if (same_lane) {
                    reference_area_size = instance.getLaneLength() -
                            instance.getSegmentLength() + position.index -
                            1;
                } else {
                    reference_area_size = instance.getLaneLength() -
                            instance.getSegmentLength() +
                            ((position.index == 0) ? (-1) : 0);
                }
            }


            /* 1.2.4. Mapping pseudoRand to 0..<reference_area_size-1> and produce
             * relative position */

            /* long in java is a signed datatype
             * we need to convert it to the unsigned value
             */

            //            relative_position = pseudoRand & 0xFFFFFFFFL;
            relative_position = pseudoRand << 32 >>> 32;
            relative_position = relative_position * relative_position;
            relative_position = relative_position >>> 32;
                    relative_position = reference_area_size - 1 - (reference_area_size * relative_position >>> 32);

                    /* 1.2.5 Computing starting position */
                    start_position = 0;

                    if (0 != position.pass) {
                        start_position = (position.slice == Constants.ARGON2_SYNC_POINTS - 1)
                                ? 0
                                        : (position.slice + 1) * instance.getSegmentLength();
                    }

                    /* 1.2.6. Computing absolute position */
                    absolute_position = (int) (start_position + relative_position) %
                            instance.getLaneLength(); /* absolute position */

                    return absolute_position;
        }

    }

    public static class Functions {

        /* H0
            argon2 -> 64 byte (ARGON2_PREHASH_DIGEST_LENGTH)
                    -> 72 byte (ARGON2_PREHASH_SEED_LENGTH)
         */
        public static byte[] initialHash(Argon2 argon2) {

            Blake2b.Param params = new Blake2b.Param()
                    .setDigestLength(Constants.ARGON2_PREHASH_DIGEST_LENGTH);

            final Blake2b blake2b = Blake2b.Digest.newInstance(params);

            blake2b.update(LittleEndian.intToBytes(argon2.getLanes()));
            blake2b.update(LittleEndian.intToBytes(argon2.getOutputLength()));
            blake2b.update(LittleEndian.intToBytes(argon2.getMemory()));
            blake2b.update(LittleEndian.intToBytes(argon2.getIterations()));
            blake2b.update(LittleEndian.intToBytes(argon2.getVersion()));
            blake2b.update(LittleEndian.intToBytes(argon2.getType().ordinal()));

            byte[] pwLengthBytes = LittleEndian.intToBytes(argon2.getPassword().length);
            blake2b.update(pwLengthBytes);
            if(argon2.getPassword() != null){
                blake2b.update(argon2.getPassword());
                argon2.clearPassword();
            }

            blake2b.update(LittleEndian.intToBytes(argon2.getSalt().length));
            if(argon2.getSalt() != null){
                blake2b.update(argon2.getSalt());
            }

            blake2b.update(LittleEndian.intToBytes(argon2.getSecret() == null ? 0 : argon2.getSecret().length));
            if(argon2.getSecret() != null){
                blake2b.update(argon2.getSecret());
                argon2.clearSecret();
            }

            blake2b.update(LittleEndian.intToBytes(argon2.getAdditional() == null ? 0 : argon2.getAdditional().length));
            if(argon2.getAdditional() != null){
                blake2b.update(argon2.getAdditional());
                argon2.clearAdditional();
            }

            byte[] blake2hash = blake2b.digest();

            // at this point, blacke2b cleaned every sensible data

            assert(blake2hash.length == 64);

            byte[] result = new byte[ARGON2_PREHASH_SEED_LENGTH];
            System.arraycopy(blake2hash, 0, result, 0, ARGON2_PREHASH_DIGEST_LENGTH);

            assert(result.length == 72);
            assert(result[64] == 0);
            assert(result[71] == 0);

            return result;
        }


        /* blake2_long - variable length hash function
         * H'
          (H0 || 0 || i) 72 byte -> 1024 byte
         */
        public static byte[] blake2bLong(byte[] input, int outlen) {

            byte[] result = new byte[outlen];
            byte[] outlen_bytes = LittleEndian.intToBytes(outlen);

            int blake2BOutbytes = 64;

            if (outlen <= blake2BOutbytes) {
                Blake2b.Param params = new Blake2b.Param()
                        .setDigestLength(outlen);

                final Blake2b blake2b = Blake2b.Digest.newInstance(params);
                blake2b.update(outlen_bytes);
                blake2b.update(input);

                result = blake2b.digest();

            } else {
                int toProduce;
                byte[] outBuffer;
                byte[] inBuffer = new byte[blake2BOutbytes];

                Blake2b.Param params = new Blake2b.Param()
                        .setDigestLength(blake2BOutbytes);

                final Blake2b blake2b = Blake2b.Digest.newInstance(params);

                blake2b.update(outlen_bytes);
                blake2b.update(input);

                outBuffer = blake2b.digest();

                System.arraycopy(outBuffer, 0, result, 0, blake2BOutbytes / 2);

                int position = blake2BOutbytes / 2;
                toProduce = outlen - blake2BOutbytes / 2;

                while (toProduce > blake2BOutbytes) {

                    System.arraycopy(outBuffer, 0, inBuffer, 0, blake2BOutbytes);

                    outBuffer = blake2b(inBuffer, blake2BOutbytes,
                            null);

                    System.arraycopy(outBuffer, 0, result, position, blake2BOutbytes / 2);


                    position += blake2BOutbytes / 2;
                    toProduce -= blake2BOutbytes / 2;
                }

                System.arraycopy(outBuffer, 0, inBuffer, 0, blake2BOutbytes);

                outBuffer = blake2b(inBuffer, toProduce, null);
                System.arraycopy(outBuffer, 0, result, position, toProduce);
            }

            assert(result.length == outlen);

            return result;
        }

        private static byte[] blake2b(byte[] input, int outlen, byte[] key){

            Blake2b.Param params = new Blake2b.Param()
                    .setDigestLength(outlen);

            if(key != null)
                params.setKey(key);

            final Blake2b blake2b = Blake2b.Digest.newInstance(params);
            blake2b.update(input);

            return blake2b.digest();
        }

        public static void roundFunction(Block block,
                int v0, int v1, int v2, int v3,
                int v4, int v5, int v6, int v7,
                int v8, int v9, int v10, int v11,
                int v12, int v13, int v14, int v15){

            G(block, v0, v4, v8, v12);
            G(block, v1, v5, v9, v13);
            G(block, v2, v6, v10, v14);
            G(block, v3, v7, v11, v15);

            G(block, v0, v5, v10, v15);
            G(block, v1, v6, v11, v12);
            G(block, v2, v7, v8, v13);
            G(block, v3, v4, v9, v14);
        }

        private static void G(Block block, int a, int b, int c, int d) {
            fBlaMka(block, a, b);
            rotr64(block, d, a, 32);

            fBlaMka(block, c, d);
            rotr64(block, b, c, 24);

            fBlaMka(block, a, b);
            rotr64(block, d, a, 16);

            fBlaMka(block, c, d);
            rotr64(block, b, c, 63);
        }

        /*designed by the Lyra PHC team */
        /* a <- a + b + 2*aL*bL
         * + == addition modulo 2^64
         * aL = least 32 bit */
        private static void fBlaMka(Block block, int x, int y) {
            final long m = 0xFFFFFFFFL;
            long xy = (block.v[x] & m) * (block.v[y] & m);

            block.v[x] =  block.v[x] + block.v[y] + 2 * xy;
        }

        private static void rotr64(Block block, int v, int w, long c) {
            long temp = block.v[v] ^ block.v[w];
            block.v[v] = (temp >>> c) | (temp << (64 - c));
        }

    }

    public interface Constants {

        public interface Defaults{

            int OUTLEN_DEF = 32;
            int T_COST_DEF = 3;
            int LOG_M_COST_DEF = 12;
            int LANES_DEF = 1;
            Type TYPE_DEF = Type.argon2i;
            int VERSION_DEF = ARGON2_VERSION_13;
        }

        /*
         * Argon2 input parameter restrictions
         */
        public interface Constraints{

            int MAX_PASSWORD_LEN = 128;

            /* Minimum and maximum number of lanes (degree of parallelism) */
            int MIN_PARALLELISM = 1;
            int MAX_PARALLELISM = 16777216;

            /* Minimum and maximum digest size in bytes */
            int MIN_OUTLEN = 4;
            int MAX_OUTLEN = Integer.MAX_VALUE;

            /* Minimum and maximum number of memory blocks (each of BLOCK_SIZE bytes) */
            int MIN_MEMORY = (2 * ARGON2_SYNC_POINTS); /* 2 blocks per slice */

            /* Minimum and maximum number of passes */
            int MIN_ITERATIONS = 1;
            int MAX_ITERATIONS = Integer.MAX_VALUE;

            /* Minimum and maximum password length in bytes */
            int MIN_PWD_LENGTH = 0;
            int MAX_PWD_LENGTH = Integer.MAX_VALUE;

            /* Minimum and maximum salt length in bytes */
            int MIN_SALT_LENGTH = 0;
            int MAX_SALT_LENGTH = Integer.MAX_VALUE;

            /* Minimum and maximum key length in bytes */
            int MAX_SECRET_LENGTH = Integer.MAX_VALUE;

            /* Minimum and maximum associated model length in bytes */
            int MAX_AD_LENGTH = Integer.MAX_VALUE;

            static void validateInput(Argon2 argon2){
                List<String> message = new LinkedList<>();

                if(argon2.getLanes() < MIN_PARALLELISM || argon2.getThreads() < MIN_PARALLELISM)
                    message.add("Degree of parallelism cannot be smaller than one");
                if(argon2.getLanes() > MAX_PARALLELISM || argon2.getThreads() > MAX_PARALLELISM)
                    message.add("Parallelism cannot be greater than 16777216");
                if(argon2.getMemory() < 2 * argon2.getLanes())
                    message.add("Memory too small");
                if(argon2.getIterations() < MIN_ITERATIONS)
                    message.add("Number of iterations cannot be less than one");
                if(argon2.getIterations() > MAX_ITERATIONS)
                    message.add("Number of iterations too high");
                if(argon2.getPassword().length < MIN_PWD_LENGTH)
                    message.add("Password too short");
                if(argon2.getPassword().length > MAX_PWD_LENGTH)
                    message.add("Password too long");
                if(argon2.getSalt().length < MIN_SALT_LENGTH)
                    message.add("Salt too short");
                if(argon2.getSalt().length > MAX_SALT_LENGTH)
                    message.add("Salt too long");
                if(argon2.getSecret() != null && argon2.getSecret().length > MAX_SECRET_LENGTH)
                    message.add("Secret too long");
                if(argon2.getAdditional() != null && argon2.getAdditional().length > MAX_AD_LENGTH)
                    message.add("Additional data too long");

                if (message.size() > 0)
                    throw new IllegalArgumentException(String.join("\n", message));
            }
        }

        /* Memory block size in bytes */
        int ARGON2_BLOCK_SIZE = 1024;
        int ARGON2_QWORDS_IN_BLOCK = ARGON2_BLOCK_SIZE / 8;
        int ARGON2_OWORDS_IN_BLOCK = ARGON2_BLOCK_SIZE / 16;

        /* Number of pseudo-random values generated by one call to Blake in Argon2i
           to
           generate reference block positions
         */
        int ARGON2_ADDRESSES_IN_BLOCK = 128;
        /* Pre-hashing digest length and its extension*/
        int ARGON2_PREHASH_DIGEST_LENGTH = 64;
        int ARGON2_PREHASH_SEED_LENGTH = 72;


        /* Number of synchronization points between lanes per pass */
        int ARGON2_SYNC_POINTS = 4;

        /* Flags to determine which fields are securely wiped (default = no wipe). */
        int ARGON2_DEFAULT_FLAGS = 0;

        int ARGON2_VERSION_10 = 0x10;
        int ARGON2_VERSION_13 = 0x13;

    }

}
