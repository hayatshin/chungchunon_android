const functions = require("firebase-functions");
const admin = require("firebase-admin");
const serviceAccount = require("./service_account_key.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://chungchunon-android-dd695-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const db = admin.firestore();

exports.commentPushNotification = functions.firestore.document("/diary/{diaryId}/comments/{diaryCommentId}").onCreate((snapShot, context) => {

    const commentUserId = snapShot.data().userId
    const commentDiaryId = snapShot.data().diaryId
    const commentDescription = snapShot.data().description

    db.collection("diary").doc(`${commentDiaryId}`).get().then((commentDiaryData) => {

        const diaryUserId = commentDiaryData.data().userId
        const diaryDescription = commentDiaryData.data().todayDiary

        db.collection("users").doc(`${commentUserId}`).get().then((commentUserData) => {
            const commentUserName = commentUserData.data().name

            db.collection("users").doc(`${diaryUserId}`).get().then((diaryUserData) => {

                const diaryUserIdFormat = diaryUserId.replace(":", "")

                 const payload = {
                               notification: {
                                   title: `일기: ${diaryDescription.slice(0, 10)}...`,
                                   body: `${commentUserName}님이 댓글을 달았습니다: ${commentDescription}`,
                               },
                           }
                  admin.messaging().sendToTopic("/topics/" + diaryUserIdFormat, payload)

            })
        })
    })

    return 0
})
