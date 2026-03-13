package com.gaiaproject.util;

/**
 * 헥스 좌표 유틸리티
 * - Axial 좌표계 (q, r) 사용
 * - 회전 변환 (60도 단위)
 */
public class HexUtil {

    private HexUtil() {
        // 유틸리티 클래스
    }

    /**
     * 헥스 좌표 회전 (60도 단위, 시계방향)
     *
     * @param q 원본 q 좌표
     * @param r 원본 r 좌표
     * @param degrees 회전 각도 (0, 60, 120, 180, 240, 300)
     * @return 회전된 좌표 [q, r]
     */
    public static int[] rotate(int q, int r, int degrees) {
        // axial → cube 변환
        int x = q;
        int z = r;
        int y = -x - z;

        // 60도 단위 회전 횟수
        int steps = (degrees / 60) % 6;

        for (int i = 0; i < steps; i++) {
            // 시계방향 60도 회전: (x, y, z) → (-z, -x, -y)
            int newX = -z;
            int newY = -x;
            int newZ = -y;
            x = newX;
            y = newY;
            z = newZ;
        }

        // cube → axial 변환
        return new int[] { x, z };
    }

    /**
     * 로컬 좌표를 글로벌 좌표로 변환
     *
     * @param localQ 로컬 q 좌표
     * @param localR 로컬 r 좌표
     * @param offsetQ 섹터 기준점 q
     * @param offsetR 섹터 기준점 r
     * @param rotation 회전 각도
     * @return 글로벌 좌표 [q, r]
     */
    public static int[] toGlobal(int localQ, int localR, int offsetQ, int offsetR, int rotation) {
        // 1. 회전 적용
        int[] rotated = rotate(localQ, localR, rotation);

        // 2. 기준점 offset 적용
        return new int[] {
            offsetQ + rotated[0],
            offsetR + rotated[1]
        };
    }

    /**
     * 두 헥스 간 거리 계산 (Axial 좌표계)
     */
    public static int distance(int q1, int r1, int q2, int r2) {
        // cube 좌표로 변환하여 계산
        int x1 = q1, z1 = r1, y1 = -x1 - z1;
        int x2 = q2, z2 = r2, y2 = -x2 - z2;

        return (Math.abs(x1 - x2) + Math.abs(y1 - y2) + Math.abs(z1 - z2)) / 2;
    }

    /**
     * 두 헥스가 인접한지 확인
     */
    public static boolean isAdjacent(int q1, int r1, int q2, int r2) {
        return distance(q1, r1, q2, r2) == 1;
    }
}
