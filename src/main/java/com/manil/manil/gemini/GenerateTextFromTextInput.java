package com.manil.manil.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GenerateTextFromTextInput{

    // 전자상거래용 상세설명 프롬프트 (한국어)
    private static final String SYSTEM_PROMPT =
            "역할: 전자상거래 카피라이터\n" +
                    "목표: 한 줄 설명을 기반으로 한국어 상품 상세설명을 작성한다.\n" +
                    "요구사항:\n" +
                    "1) 250~400자, 3~6문장으로 간결하고 정보 중심으로 작성할 것.\n" +
                    "2) 핵심 특징(소재/기능/호환/사이즈 등), 효용, 사용 시나리오를 포함할 것.\n" +
                    "3) 확정되지 않은 정보는 단정하지 말고 ‘~일 수 있습니다/ ~로 보입니다’처럼 책임 있는 어투 사용.\n" +
                    "4) 과장·과도한 마케팅 표현·이모지·특수기호 금지, 명료한 문장 위주.\n" +
                    "5) 구매 결정에 필요한 구체성(관리 방법, 주의사항 등)이 있으면 덧붙일 것.\n" +
                    "6) 유사제품에 대한 만족도를 구체적으로 서술할 것\n" +
                    "7) 문장마다 끊어서 한줄로 쓰지 않고, 한 문장을 기준으로 여러문단(여러줄)으로 만들어서 가시성 좋게 문장을 구성할것" ;

    public static void main(String[] args) {
        // 콘솔 입력 준비
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print("상품 한줄 설명을 입력하세요: ");
        String userLine = sc.nextLine().trim();

        if (userLine.isEmpty()) {
            System.err.println("입력이 비어 있습니다. 프로그램을 종료합니다.");
            return;
        }

        // Google GenAI 클라이언트 (환경변수 GEMINI_API_KEY 필요)
        Client client;
        try {
            client = new Client(); // GEMINI_API_KEY를 환경변수에서 자동으로 읽음
        } catch (Exception e) {
            System.err.println("Client 생성 실패: GEMINI_API_KEY 환경변수를 설정했는지 확인하세요.");
            return;
        }

        // 프롬프트 구성
        String prompt =
                SYSTEM_PROMPT +
                        "\n[한 줄 설명]\n" + userLine + "\n\n" +
                        "[작성 지시]\n위 요구사항을 모두 반영하여 한국어 상품 상세설명을 만들어 주세요.";

        try {
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
                    null
            );

            String text = response.text();
            if (text == null || text.isBlank()) {
                System.out.println("모델이 빈 응답을 반환했습니다.");
            } else {
                System.out.println("\n=== 생성된 상세설명 ===\n" + text.trim());
            }
        } catch (Exception e) {
            System.err.println("텍스트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
