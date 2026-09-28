import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { JWT } from "https://esm.sh/google-auth-library@9";

async function getAccessToken(serviceAccount: any) {
  const client = new JWT({
    email: serviceAccount.client_email,
    key: serviceAccount.private_key,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  const tokens = await client.authorize();
  return tokens.access_token;
}

serve(async (req) => {
  try {
    // 1. Kiểm tra secret
    const serviceAccountRaw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");
    if (!serviceAccountRaw) {
      console.error("LỖI: Chưa cấu hình secret FIREBASE_SERVICE_ACCOUNT");
      return new Response(JSON.stringify({ error: "Missing FIREBASE_SERVICE_ACCOUNT secret" }), { status: 500 });
    }
    const serviceAccount = JSON.parse(serviceAccountRaw);

    // 2. Kiểm tra body nhận từ Webhook
    const payload = await req.json();
    console.log("Payload nhận được từ Webhook:", JSON.stringify(payload));

    const record = payload.record;
    if (!record) {
      console.error("LỖI: Payload không có record", payload);
      return new Response(JSON.stringify({ error: "No record found in payload" }), { status: 400 });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    // 3. Tìm thành viên nhận thông báo trong conversation
    const { data: members, error: membersError } = await supabase
      .from("conversation_members")
      .select("user_id")
      .eq("conversation_id", record.conversation_id)
      .neq("user_id", record.sender_id);

    if (membersError) {
      console.error("LỖI query conversation_members:", membersError);
      return new Response(JSON.stringify({ error: membersError.message }), { status: 500 });
    }

    if (!members || members.length === 0) {
      console.log("Không có người nhận phù hợp (hoặc người gửi là thành viên duy nhất).");
      return new Response(JSON.stringify({ message: "No recipients" }), { status: 200 });
    }

    const memberIds = members.map((m) => m.user_id);
    console.log("Danh sách memberIds nhận tin:", memberIds);

    // 4. Lấy FCM Tokens
    const { data: tokens, error: tokensError } = await supabase
      .from("user_fcm_tokens")
      .select("fcm_token")
      .in("user_id", memberIds);

    if (tokensError) {
      console.error("LỖI query user_fcm_tokens:", tokensError);
      return new Response(JSON.stringify({ error: tokensError.message }), { status: 500 });
    }

    if (!tokens || tokens.length === 0) {
      console.log("Không tìm thấy FCM token nào cho các thành viên này.");
      return new Response(JSON.stringify({ message: "No tokens found" }), { status: 200 });
    }

    // 5. Lấy tên người gửi
    const { data: sender } = await supabase
      .from("users")
      .select("display_name")
      .eq("id", record.sender_id)
      .single();

    const senderName = sender?.display_name || "Tin nhắn mới";
    const accessToken = await getAccessToken(serviceAccount);

    // 6. Gửi tới từng FCM token
    const results = [];
    for (const t of tokens) {
      const res = await fetch(
        `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token: t.fcm_token,
              notification: {
                title: senderName,
                body: record.content || "Đã gửi một tệp đính kèm",
              },
              data: {
                conversation_id: record.conversation_id,
              },
            },
          }),
        }
      );
      const resJson = await res.json();
      console.log("FCM Response:", resJson);
      results.push(resJson);
    }

    return new Response(JSON.stringify({ success: true, results }), { status: 200 });
  } catch (err: any) {
    console.error("LỖI CRASH TRONG HÀM:", err.message, err.stack);
    return new Response(JSON.stringify({ error: err.message, stack: err.stack }), { status: 500 });
  }
});