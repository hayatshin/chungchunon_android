const functions = require("firebase-functions");
const admin = require("firebase-admin");
const serviceAccount = require("./service_account_key.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://chungchunon-android-dd695-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const db = admin.firestore();

exports.commentPushNotification = functions.firestore.document("/diary/{diaryId}/comments/{diaryCommentId}").onCreate((snapShot, context) => {

    const commentId = snapShot.data().commentId
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
                const titleReformat = (diaryDescription.length > 10) ? `${diaryDescription.slice(0, 10)}...` : diaryDescription
                const commentDescriptionReformat = (commentDescription.length > 15) ? `${commentDescription.slice(0, 15)}...` : commentDescription

                 const payload = {
                               notification: {
                                   title: `일기: ${titleReformat}`,
                                   body: `${commentUserName}님이 댓글을 달았습니다: ${commentDescriptionReformat}`,
                                   clickAction: "DiaryTwoActivity"
                               },
                               data: {
                                    notificationDiaryId: commentDiaryId,
                                    notificationCommentId: commentId
                               }
                           }
                  admin.messaging().sendToTopic(diaryUserIdFormat, payload)

            })
        })
    })
    return 0
})

exports.increaseLikeNum = functions.firestore.document("/diary/{diaryId}/likes/{diaryLikeUserId}").onCreate((snapShot, context) => {
    const diaryId = snapShot.data().diaryId
    const diaryLikeUserId = snapShot.data().diaryLikeUserId

     db.collection("diary").doc(`${diaryId}`).update({
        numLikes: admin.firestore.FieldValue.increment(1)
    })
    return 0
})

exports.decreaseLikeNum = functions.firestore.document("/diary/{diaryId}/likes/{diaryLikeUserId}").onDelete((snapShot, context) => {
     const diaryId = snapShot.data().diaryId
    const diaryLikeUserId = snapShot.data().diaryLikeUserId

    db.collection("diary").doc(`${diaryId}`).update({
        numLikes: admin.firestore.FieldValue.increment(-1)
    })
    return 0
})

exports.increaseCommentNum = functions.firestore.document("/diary/{diaryId}/comments/{diaryCommentUserId}").onCreate((snapShot, context) => {
     const diaryId = snapShot.data().diaryId

    db.collection("diary").doc(`${diaryId}`).update({
        numComments: admin.firestore.FieldValue.increment(1)
    })
    return 0
})

exports.decreaseCommentNum = functions.firestore.document("/diary/{diaryId}/comments/{diaryCommentUserId}").onDelete((snapShot, context) => {
     const diaryId = snapShot.data().diaryId

    db.collection("diary").doc(`${diaryId}`).update({
        numComments: admin.firestore.FieldValue.increment(-1)
    })
    return 0
})


