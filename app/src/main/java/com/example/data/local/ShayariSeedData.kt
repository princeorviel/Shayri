package com.example.data.local

import com.example.data.local.seed.SeedAansuAndMusafir
import com.example.data.local.seed.SeedAttitude
import com.example.data.local.seed.SeedBewafai
import com.example.data.local.seed.SeedBonusExisting
import com.example.data.local.seed.SeedFestive
import com.example.data.local.seed.SeedGenZ
import com.example.data.local.seed.SeedJijaSaliAndMulla
import com.example.data.local.seed.SeedKhudaRab
import com.example.data.local.seed.SeedMautJanaza
import com.example.data.local.seed.SeedNibbaNibbi
import com.example.data.local.seed.SeedRomantic
import com.example.data.local.seed.SeedSantaBantaAndMotuPatlu
import com.example.data.local.seed.SeedSecretLove
import com.example.data.local.seed.SeedSharabi
import com.example.data.local.seed.SeedTareefAndAaina
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object ShayariSeedData {
    private val classicShayari: List<ShayariEntity> = listOf(
        // ==================== प्यार (MOHABBAT) - 14 ====================
        ShayariEntity(
            id = 1,
            couplet = "इश्क़ ने 'ग़ालिब' निकम्मा कर दिया,\nवरना हम भी आदमी थे काम के।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "मोहब्बत • क्लासिक",
            isFeatured = true
        ),
        ShayariEntity(
            id = 2,
            couplet = "उनके देखे से जो आ जाती है मुँह पर रौनक़,\nवो समझते हैं कि बीमार का हाल अच्छा है।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "दीदार • प्यार"
        ),
        ShayariEntity(
            id = 3,
            couplet = "मोहब्बत में नहीं है फ़र्क़ जीने और मरने का,\nउसी को देख कर जीते हैं जिस काफ़िर पे दम निकले।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "शिद्दत • इश्क़"
        ),
        ShayariEntity(
            id = 4,
            couplet = "कहाँ मैं और कहाँ ये नख़वत-ए-दिल,\nमगर तेरी नज़र ने कर दिया क्या।",
            poet = "अमीर ख़ुसरो",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "नज़र • प्यार"
        ),
        ShayariEntity(
            id = 5,
            couplet = "तेरे मिलने की आस रहती है,\nज़िंदगी ख़ुश-लिबास रहती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "इंतज़ार • प्यार"
        ),
        ShayariEntity(
            id = 6,
            couplet = "तुम पास होते हो गोया कोई दूसरा नहीं होता,\nदिल की हर धड़कन में बस तेरा ही नाम होता।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "क़ुर्बत • मोहब्बत"
        ),
        ShayariEntity(
            id = 7,
            couplet = "उसने पूछा कि क्या पसंद है तुम्हें,\nमैं बहुत देर तक उसे देखता रहा।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "इज़हार • प्यार"
        ),
        ShayariEntity(
            id = 8,
            couplet = "इश्क़ पर ज़ोर नहीं है ये वो आतिश 'ग़ालिब',\nकि लगाए न लगे और बुझाए न बने।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "असर • इश्क़"
        ),
        ShayariEntity(
            id = 9,
            couplet = "हज़ारों ख़्वाहिशें ऐसी कि हर ख़्वाहिश पे दम निकले,\nबहुत निकले मेरे अरमान लेकिन फिर भी कम निकले।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "आरज़ू • प्यार"
        ),
        ShayariEntity(
            id = 64,
            couplet = "तेरी मुस्कान से रोशन है मेरा सारा जहाँ,\nतू साथ हो तो हर मौसम लगता है मेहरबाँ।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "मुस्कान • प्यार"
        ),
        ShayariEntity(
            id = 65,
            couplet = "आँखों से आँखों का ये कैसा रिश्ता बन गया,\nबिना लफ़्ज़ों के ही पूरा अफ़साना बन गया।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "अफ़साना • मोहब्बत"
        ),
        ShayariEntity(
            id = 66,
            couplet = "चाहने वालों की कमी नहीं है इस ज़माने में,\nपर जो रूह को छू जाए वो प्यार एक ही होता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "रूहानी प्यार"
        ),
        ShayariEntity(
            id = 67,
            couplet = "हाथ थाम कर चलना तो सब सीख लेते हैं,\nसच्चा प्यार वो है जो ख़ामोशी भी समझ ले।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "एहसास • प्यार"
        ),
        ShayariEntity(
            id = 68,
            couplet = "सुबह की पहली किरण-सा है तेरा ख़याल,\nदिन चाहे जैसा भी हो मन महक ही जाता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "ख़याल • मोहब्बत"
        ),

        // ==================== दर्द (DARD) - 12 ====================
        ShayariEntity(
            id = 10,
            couplet = "ये न थी हमारी क़िस्मत कि विसाल-ए-यार होता,\nअगर और जीते रहते यही इंतज़ार होता।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "जुदाई"
        ),
        ShayariEntity(
            id = 11,
            couplet = "कोई उम्मीद बर नहीं आती,\nकोई सूरत नज़र नहीं आती।\nमौत का एक दिन मुअय्यन है,\nनींद क्यों रात भर नहीं आती।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "बेचैनी"
        ),
        ShayariEntity(
            id = 12,
            couplet = "तुम्हारे शहर का मौसम बड़ा सुहाना लगे,\nमैं एक शाम चुरा लूँ अगर बुरा न लगे।",
            poet = "क़ैसर-उल-जाफ़री",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "हसरत"
        ),
        ShayariEntity(
            id = 13,
            couplet = "दिल ही तो है न संग-ओ-ख़िश्त दर्द से भर न आए क्यों,\nरोएंगे हम हज़ार बार कोई हमें सताए क्यों।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "एहसास"
        ),
        ShayariEntity(
            id = 14,
            couplet = "रंजिश ही सही दिल ही दुखाने के लिए आ,\nआ फिर से मुझे छोड़ के जाने के लिए आ।",
            poet = "अहमद फ़राज़",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "रंजिश"
        ),
        ShayariEntity(
            id = 15,
            couplet = "वो जो हम में तुम में क़रार था तुम्हें याद हो कि न याद हो,\nवही यानी वादा निबाह का तुम्हें याद हो कि न याद हो।",
            poet = "मोमिन ख़ाँ मोमिन",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "शिकवा"
        ),
        ShayariEntity(
            id = 16,
            couplet = "ख़ामोश लब हैं झुकी हैं पलकें,\nदिल में अश्कों का समंदर लिए बैठे हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "ख़ामोशी"
        ),
        ShayariEntity(
            id = 17,
            couplet = "टूटे हुए ख़्वाबों ने हमको ये सिखाया है,\nदिल ने जिसे चाहा अक्सर वही पराया है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "दिल"
        ),
        ShayariEntity(
            id = 18,
            couplet = "अश्कों को आँखों में छुपाने का हुनर आ गया,\nमुस्कुराकर हर दर्द भुलाने का हुनर आ गया।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "सब्र"
        ),
        ShayariEntity(
            id = 69,
            couplet = "कुछ ज़ख़्म ऐसे होते हैं जो दिखते नहीं हैं,\nबस अंदर ही अंदर पूरी उम्र टीसते रहते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "टीस • दर्द"
        ),
        ShayariEntity(
            id = 70,
            couplet = "हँसते हुए चेहरे के पीछे क्या छुपा है कौन जाने,\nहर किसी के हिस्से में कोई अनकहा दर्द होता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "अनकहा दर्द"
        ),
        ShayariEntity(
            id = 71,
            couplet = "बिछड़ कर तुझसे ये एहसास हुआ,\nदिल के शहर में कितना सन्नाटा है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DARD.id,
            moodTag = "बिछोह"
        ),

        // ==================== ज़िंदगी (ZINDAGI) - 12 ====================
        ShayariEntity(
            id = 19,
            couplet = "ज़िंदगी यूँ हुई बसर तन्हा,\nक़ाफ़िला साथ था और सफ़र तन्हा।",
            poet = "गुलज़ार",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "सफ़र",
            isFeatured = true
        ),
        ShayariEntity(
            id = 20,
            couplet = "उम्र-ए-दराज़ माँग के लाए थे चार दिन,\nदो आरज़ू में कट गए दो इंतज़ार में।",
            poet = "सीमाब अकबराबादी",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "वक़्त"
        ),
        ShayariEntity(
            id = 21,
            couplet = "दुनिया जिसे कहते हैं जादू का खिलौना है,\nमिल जाए तो मिट्टी है खो जाए तो सोना है।",
            poet = "निदा फ़ाज़ली",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "फ़लसफ़ा"
        ),
        ShayariEntity(
            id = 22,
            couplet = "ग़ालिब बुरा न मान जो वाइज़ बुरा कहे,\nऐसा भी कोई है कि सब अच्छा कहें जिसे।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "तजुर्बा"
        ),
        ShayariEntity(
            id = 23,
            couplet = "सफ़र में धूप तो होगी जो चल सको तो चलो,\nसभी हैं भीड़ में तुम भी निकल सको तो चलो।",
            poet = "निदा फ़ाज़ली",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "राह"
        ),
        ShayariEntity(
            id = 24,
            couplet = "वक़्त करता है परवरिश बरसों,\nहादसा एक दम नहीं होता।",
            poet = "बशीर बद्र",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "हक़ीक़त"
        ),
        ShayariEntity(
            id = 25,
            couplet = "कभी किसी को मुकम्मल जहाँ नहीं मिलता,\nकहीं ज़मीं तो कहीं आसमाँ नहीं मिलता।",
            poet = "निदा फ़ाज़ली",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "मुकम्मल"
        ),
        ShayariEntity(
            id = 26,
            couplet = "ज़िंदगी की असली ख़ूबसूरती इसमें नहीं कि हम कितने ख़ुश हैं,\nबल्कि इसमें है कि हमारी वजह से कितने लोग ख़ुश हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "सुकून"
        ),
        ShayariEntity(
            id = 27,
            couplet = "आईना देख कर तसल्ली हुई,\nहमको इस घर में जानता है कोई।",
            poet = "गुलज़ार",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "आईना"
        ),
        ShayariEntity(
            id = 72,
            couplet = "ज़िंदगी ने हर मोड़ पर नया सबक़ सिखाया,\nकभी गिरकर संभलना तो कभी मुस्कुराना सिखाया।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "सबक़"
        ),
        ShayariEntity(
            id = 73,
            couplet = "छोटी-छोटी ख़ुशियों से ही सजती है ज़िंदगी,\nबड़े ख़्वाबों में अक्सर आज छूट जाता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "आज"
        ),
        ShayariEntity(
            id = 74,
            couplet = "नदी की तरह बहती रहे ज़िंदगी तो बेहतर है,\nठहरा हुआ पानी अक्सर अपना रंग खो देता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.ZINDAGI.id,
            moodTag = "प्रवाह"
        ),

        // ==================== दोस्ती (DOSTI) - 14 ====================
        ShayariEntity(
            id = 28,
            couplet = "दोस्ती आम है लेकिन ऐ दोस्त,\nदोस्त मिलता है बड़ी मुश्किल से।",
            poet = "हफ़ीज़ होशियारपुरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "सच्ची यारी"
        ),
        ShayariEntity(
            id = 29,
            couplet = "रिश्तों की भीड़ में कुछ नाम ख़ास होते हैं,\nदूर रहकर भी जो दिल के सबसे पास होते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "अपनापन"
        ),
        ShayariEntity(
            id = 30,
            couplet = "वफ़ा के नाम पर कुछ दोस्त ऐसे भी मिले,\nजिन्होंने हर मोड़ पर हँसकर गले लगा लिया।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "वफ़ा"
        ),
        ShayariEntity(
            id = 31,
            couplet = "मुस्कान होठों पर और हाथ कंधे पर रहे,\nसच्चा यार वही जो हर मुश्किल में साथ खड़ा रहे।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "साथ"
        ),
        ShayariEntity(
            id = 32,
            couplet = "न जाने किस बात की सजा दे रहा है वक़्त,\nपर यारों की महफ़िल में आज भी सुकून मिलता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "महफ़िल"
        ),
        ShayariEntity(
            id = 33,
            couplet = "दोस्ती वो नहीं जो जान देती है,\nदोस्ती वो है जो जीने की वजह देती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "उम्मीद"
        ),
        ShayariEntity(
            id = 34,
            couplet = "लफ़्ज़ों की ज़रूरत नहीं पड़ती यारी में,\nख़ामोशी भी अक्सर हाल बयाँ कर देती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "भरोसा"
        ),
        ShayariEntity(
            id = 35,
            couplet = "तन्हाइयों में भी मुस्कुराने की आदत है हमें,\nक्योंकि दुआओं में शामिल कुछ प्यारे दोस्त हैं मेरे।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "दुआ"
        ),
        ShayariEntity(
            id = 36,
            couplet = "क़द और दौलत से नहीं नापी जाती दोस्ती,\nये तो दिलों के मिलने का खूबसूरत एहसास है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "एहसास"
        ),
        ShayariEntity(
            id = 75,
            couplet = "बरसों बाद भी जब मिलते हैं पुराने दोस्त,\nवक़्त मानो वहीं से फिर मुस्कुराने लगता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "पुराने दोस्त"
        ),
        ShayariEntity(
            id = 76,
            couplet = "चाय की प्याली और यारों की बेफ़िक्र हँसी,\nमहँगी से महँगी दावत भी इनके आगे फीकी है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "यारों की हँसी"
        ),
        ShayariEntity(
            id = 77,
            couplet = "आईने की तरह होते हैं सच्चे दोस्त,\nहमेशा सच दिखाते हैं पर कभी तोड़ते नहीं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "सच्चाई"
        ),
        ShayariEntity(
            id = 78,
            couplet = "धूप में छाँव और तूफ़ान में सहारा बन जाए,\nख़ुदा हर किसी की झोली में ऐसा एक दोस्त डाल दे।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "सहारा"
        ),
        ShayariEntity(
            id = 79,
            couplet = "न कोई शर्त न कोई हिसाब होता है,\nदोस्ती का रिश्ता तो बस बेमिसाल होता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DOSTI.id,
            moodTag = "बेमिसाल यारी"
        ),

        // ==================== प्रेरणा / हौसला (HAUSLA) - 14 ====================
        ShayariEntity(
            id = 37,
            couplet = "कौन कहता है आसमाँ में सुराख़ नहीं हो सकता,\nएक पत्थर तो तबीयत से उछालो यारो।",
            poet = "दुष्यंत कुमार",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "प्रेरणा • जज़्बा",
            isFeatured = true
        ),
        ShayariEntity(
            id = 38,
            couplet = "हो गई है पीर पर्वत-सी पिघलनी चाहिए,\nइस हिमालय से कोई गंगा निकलनी चाहिए।",
            poet = "दुष्यंत कुमार",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "प्रेरणा • संकल्प"
        ),
        ShayariEntity(
            id = 39,
            couplet = "मेरे सीने में नहीं तो तेरे सीने में सही,\nहो कहीं भी आग लेकिन आग जलनी चाहिए।",
            poet = "दुष्यंत कुमार",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "चिंगारी • प्रेरणा"
        ),
        ShayariEntity(
            id = 40,
            couplet = "सितारों से आगे जहाँ और भी हैं,\nअभी इश्क़ के इम्तिहाँ और भी हैं।",
            poet = "अल्लामा इक़बाल",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "परवाज़ • प्रेरणा"
        ),
        ShayariEntity(
            id = 41,
            couplet = "तू शाहीं है परवाज़ है काम तेरा,\nतेरे सामने आसमाँ और भी हैं।",
            poet = "अल्लामा इक़बाल",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "उड़ान • हौसला"
        ),
        ShayariEntity(
            id = 42,
            couplet = "ख़ुदी को कर बुलंद इतना कि हर तक़दीर से पहले,\nख़ुदा बंदे से ख़ुद पूछे बता तेरी रज़ा क्या है।",
            poet = "अल्लामा इक़बाल",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "आत्मविश्वास"
        ),
        ShayariEntity(
            id = 43,
            couplet = "लहरों से डर कर नौका पार नहीं होती,\nकोशिश करने वालों की कभी हार नहीं होती।",
            poet = "सोहन लाल द्विवेदी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "हिम्मत • प्रेरणा"
        ),
        ShayariEntity(
            id = 44,
            couplet = "मंज़िलें उन्हीं को मिलती हैं जिनके सपनों में जान होती है,\nपंख से कुछ नहीं होता हौसलों से उड़ान होती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "मंज़िल • हौसला"
        ),
        ShayariEntity(
            id = 45,
            couplet = "गिरना भी अच्छा है औक़ात पता चलती है,\nबढ़ते हैं जब हाथ उठाने को तो अपनों का पता चलता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "सीख"
        ),
        ShayariEntity(
            id = 80,
            couplet = "राह संघर्ष की जो चलता है वही संसार को बदलता है,\nजिसने रातों से जंग जीती है सुबह सूर्य बनकर वही चमकता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "संघर्ष • प्रेरणा"
        ),
        ShayariEntity(
            id = 81,
            couplet = "अँधेरों से मत डरना ऐ मुसाफ़िर,\nसितारे अक्सर घनी रात में ही चमकते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "उम्मीद • प्रेरणा"
        ),
        ShayariEntity(
            id = 82,
            couplet = "क़दम बढ़ाते चलो चाहे रास्ते पथरीले हों,\nइरादे मज़बूत हों तो चट्टानें भी रास्ता दे देती हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "इरादा • हौसला"
        ),
        ShayariEntity(
            id = 83,
            couplet = "हार से सीख लेकर जो फिर से उठ खड़ा होता है,\nज़माना एक दिन उसी के क़दमों में झुकता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "जीत • प्रेरणा"
        ),
        ShayariEntity(
            id = 84,
            couplet = "सपने वो नहीं जो नींद में आएँ,\nसपने वो हैं जो आपको सोने न दें।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.HAUSLA.id,
            moodTag = "लक्ष्य • प्रेरणा"
        ),

        // ==================== तन्हाई (TANHAI) - 12 ====================
        ShayariEntity(
            id = 46,
            couplet = "हमने देखी है उन आँखों की महकती ख़ुशबू,\nहाथ से छू के इसे रिश्तों का इल्ज़ाम न दो।",
            poet = "गुलज़ार",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "नज़ाकत"
        ),
        ShayariEntity(
            id = 47,
            couplet = "हज़ारों साल नर्गिस अपनी बेनूरी पे रोती है,\nबड़ी मुश्किल से होता है चमन में दीदावर पैदा।",
            poet = "अल्लामा इक़बाल",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "अकेलापन"
        ),
        ShayariEntity(
            id = 48,
            couplet = "दिल की बात लबों पर लाकर अब तक हम दुख सहते हैं,\nहमने सुना था इस बस्ती में दिल वाले भी रहते हैं।",
            poet = "फ़ैज़ अहमद फ़ैज़",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "सन्नाटा"
        ),
        ShayariEntity(
            id = 49,
            couplet = "रात भर जागता रहा चाँद मेरी खिड़की के पास,\nशायद उसे भी किसी अपने का इंतज़ार था।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "चाँदनी"
        ),
        ShayariEntity(
            id = 50,
            couplet = "भीड़ में भी एक अजीब-सा सन्नाटा है अंदर,\nजैसे कोई अपने ही घर में मुसाफ़िर हो जाए।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "ख़लिश"
        ),
        ShayariEntity(
            id = 51,
            couplet = "जब कोई बात बिगड़ जाए जब कोई मुश्किल पड़ जाए,\nतन्हाई ही बन जाती है अक्सर सबसे गहरी हमराज़।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "हमराज़"
        ),
        ShayariEntity(
            id = 52,
            couplet = "चुपचाप गुज़र जाते हैं शाम-ओ-सहर तेरे बिन,\nजैसे वक़्त ने भी थाम ली हो रफ़्तार अपनी।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "शाम"
        ),
        ShayariEntity(
            id = 53,
            couplet = "ख़ुद से गुफ़्तगू करने का मज़ा ही कुछ और है,\nयहाँ न कोई गिला होता है न कोई झूठा दिलासा।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "सुकून"
        ),
        ShayariEntity(
            id = 54,
            couplet = "दिया जलता रहा रात भर मेरे कमरे में,\nअँधेरे से लड़ने की ज़िद अभी बाक़ी है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "चराग़"
        ),
        ShayariEntity(
            id = 85,
            couplet = "तन्हाई में ही इंसान ख़ुद से सही मायने में मिलता है,\nशोर की दुनिया में तो बस चेहरे बदले जाते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "आत्मचिंतन"
        ),
        ShayariEntity(
            id = 86,
            couplet = "ख़ाली कमरा और घड़ी की टिक-टिक की आवाज़,\nरात के सन्नाटे में हर ख़याल गूँजने लगता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "रात"
        ),
        ShayariEntity(
            id = 87,
            couplet = "बंद किताब की तरह हो गई है ज़िंदगी कभी-कभी,\nपन्ने तो बहुत हैं पर पढ़ने वाला कोई नहीं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.TANHAI.id,
            moodTag = "किताब"
        ),

        // ==================== यादें (YAADEIN) - 12 ====================
        ShayariEntity(
            id = 55,
            couplet = "कुछ तो मजबूरियाँ रही होंगी,\nयूँ कोई बेवफ़ा नहीं होता।",
            poet = "बशीर बद्र",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "एहतिराम"
        ),
        ShayariEntity(
            id = 56,
            couplet = "उजाले अपनी यादों के हमारे साथ रहने दो,\nन जाने किस गली में ज़िंदगी की शाम हो जाए।",
            poet = "बशीर बद्र",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "उजाला"
        ),
        ShayariEntity(
            id = 57,
            couplet = "मुसाफ़िर हैं हम भी मुसाफ़िर हो तुम भी,\nकिसी मोड़ पर फिर मुलाक़ात होगी।",
            poet = "बशीर बद्र",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "मुलाक़ात"
        ),
        ShayariEntity(
            id = 58,
            couplet = "क़ाफ़िले यादों के जब दिल से गुज़रते हैं,\nगुज़रे हुए मौसम फिर से हरे हो जाते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "मौसम"
        ),
        ShayariEntity(
            id = 59,
            couplet = "पुरानी किताबों में रखा सूखा गुलाब,\nआज भी तेरे साथ गुज़रे वक़्त की महक देता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "गुलाब"
        ),
        ShayariEntity(
            id = 60,
            couplet = "तस्वीरें बोलती नहीं पर सब कुछ कह जाती हैं,\nगुज़रे हुए कल को आँखों में ज़िंदा रख जाती हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "तस्वीर"
        ),
        ShayariEntity(
            id = 61,
            couplet = "बारिश की बूँदों में भी छुपी होती हैं कुछ कहानियाँ,\nभीगती मिट्टी से आती है बचपन की यादों की ख़ुशबू।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "बारिश"
        ),
        ShayariEntity(
            id = 62,
            couplet = "भूल जाना भी एक नेमत है मगर,\nकुछ चेहरे दिल की दहलीज़ से जाते ही नहीं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "दहलीज़"
        ),
        ShayariEntity(
            id = 63,
            couplet = "वक़्त आगे निकल गया कब का,\nदिल वहीं ठहरा है जहाँ तुम मिले थे।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "ठहराव"
        ),
        ShayariEntity(
            id = 88,
            couplet = "बचपन के वो काग़ज़ की कश्ती वाले दिन,\nआज भी बारिश आने पर बहुत याद आते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "बचपन"
        ),
        ShayariEntity(
            id = 89,
            couplet = "कुछ बातें पुरानी होकर भी नई रहती हैं,\nदिल के किसी कोने में चुपचाप धड़कती रहती हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "धड़कन"
        ),
        ShayariEntity(
            id = 90,
            couplet = "गुज़रे हुए लम्हों का सफ़र ख़त्म नहीं होता,\nयादों के शहर में हर शख़्स ज़िंदा रहता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "लम्हे"
        ),

        // ==================== प्रकृति (PRAKRITI) - 14 ====================
        ShayariEntity(
            id = 91,
            couplet = "बूंदें गिरीं तो जाग उठी मिट्टी की ख़ुशबू,\nकुदरत ने फिर लिखा ज़मीं पर सावन का नाम।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "प्रकृति • सावन",
            isFeatured = true
        ),
        ShayariEntity(
            id = 92,
            couplet = "हवाओं के झोंके जब पत्तों से गुज़रते हैं,\nलगता है जंगल भी कोई ग़ज़ल गुनगुना रहा है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "हवा • जंगल"
        ),
        ShayariEntity(
            id = 93,
            couplet = "सुबह की ओस में भीगी हुई हर एक कली,\nचुपचाप कहती है कि नया दिन मुस्कुराने वाला है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "सुबह • ओस"
        ),
        ShayariEntity(
            id = 94,
            couplet = "पहाड़ों की चोटी पर ठहरा हुआ सफ़ेद बादल,\nजैसे आसमाँ ने धरती के सिर पर ताज सजाया हो।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "पहाड़ • बादल"
        ),
        ShayariEntity(
            id = 95,
            couplet = "बहती नदियों ने कभी मंज़िल की शिकायत नहीं की,\nपत्थरों से टकराकर भी अपना गीत गाती रहीं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "नदी • प्रकृति"
        ),
        ShayariEntity(
            id = 96,
            couplet = "चाँदनी रात में जब झील का पानी चमकता है,\nलगता है आसमाँ का हर सितारा ज़मीं पर उतर आया।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "चाँदनी • झील"
        ),
        ShayariEntity(
            id = 97,
            couplet = "फूलों ने काँटों के बीच रहकर भी महकना नहीं छोड़ा,\nकुदरत सिखाती है कि हालात जैसे भी हों मुस्कराते रहो।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "फूल • प्रकृति"
        ),
        ShayariEntity(
            id = 98,
            couplet = "ढलते सूरज ने समंदर को सुनहरा रंग दे दिया,\nशाम की ख़ामोशी ने हर दिल को सुकून दे दिया।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "सूरज • समंदर"
        ),
        ShayariEntity(
            id = 99,
            couplet = "परिंदों की चहचहाहट से खुलती है गाँव की सुबह,\nशहर के शोर से दूर यहाँ हर साँस ताज़ा लगती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "परिंदे • गाँव"
        ),
        ShayariEntity(
            id = 100,
            couplet = "हरियाली की चादर ओढ़े जब झूमती हैं वादियाँ,\nथका हुआ मुसाफ़िर भी सारी उदासी भूल जाता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "वादियाँ • हरियाली"
        ),
        ShayariEntity(
            id = 101,
            couplet = "पेड़ों ने कभी अपने साये का मोल नहीं माँगा,\nबस चुपचाप धूप सहकर मुसाफ़िरों को राहत दी।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "पेड़ • छाँव"
        ),
        ShayariEntity(
            id = 102,
            couplet = "इंद्रधनुष के सात रंग जब आसमाँ में बिखरते हैं,\nबारिश के बाद की दुनिया और भी हसीन लगती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "इंद्रधनुष • बारिश"
        ),
        ShayariEntity(
            id = 103,
            couplet = "झरनों के संगीत में कोई बनावट नहीं होती,\nये कुदरत की वो धुन है जो सीधे रूह तक जाती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "झरना • संगीत"
        ),
        ShayariEntity(
            id = 104,
            couplet = "खुले आसमाँ के नीचे जब ठंडी बयार चलती है,\nसारी दुनिया की फ़िक्रें एक पल में उड़ जाती हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PRAKRITI.id,
            moodTag = "आसमाँ • हवा"
        ),

        // ==================== ख़ुशी व मुस्कान (KHUSHI) - 12 ====================
        ShayariEntity(
            id = 105,
            couplet = "मुस्कुराने की वजह न ढूँढो ज़माने में,\nजिन्हें जीना आता है वो हर हाल में मुस्कुराते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "मुस्कान • ख़ुशी"
        ),
        ShayariEntity(
            id = 106,
            couplet = "एक छोटी-सी हँसी से दिन संवर जाता है,\nजब दिल ख़ुश हो तो हर रास्ता आसान नज़र आता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "हँसी"
        ),
        ShayariEntity(
            id = 107,
            couplet = "ख़ुशियाँ बाँटने से कम नहीं होतीं कभी,\nये वो दौलत है जो लुटाने पर और बढ़ती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "ख़ुशियाँ"
        ),
        ShayariEntity(
            id = 108,
            couplet = "शुक्र अदा कर उस हर सुबह का जो आँख खुलते ही मिली,\nज़िंदगी का असली जश्न हर पल को जी लेने में है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "शुक्रगुज़ारी"
        ),
        ShayariEntity(
            id = 109,
            couplet = "गम के बादल छँट ही जाते हैं एक दिन,\nबस दिल में उम्मीद का सूरज जलाए रखो।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "सकारात्मकता"
        ),
        ShayariEntity(
            id = 110,
            couplet = "मीठे बोल और प्यारी-सी मुस्कान,\nयही तो हैं किसी भी इंसान की सबसे बड़ी पहचान।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "पहचान"
        ),
        ShayariEntity(
            id = 111,
            couplet = "कल की चिंता छोड़ आज को गले लगा ले,\nख़ुशी तेरे अंदर ही है बस नज़र घुमा ले।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "आज का पल"
        ),
        ShayariEntity(
            id = 112,
            couplet = "बच्चों की खिलखिलाहट सुनकर दिल बाग़-बाग़ हो जाता है,\nसारी थकान पल भर में जैसे छूमंतर हो जाती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "मासूमियत"
        ),
        ShayariEntity(
            id = 113,
            couplet = "जब मन शांत हो और चेहरा मुस्कुराता रहे,\nतो साधारण-सा दिन भी त्यौहार बन जाता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "उल्लास"
        ),
        ShayariEntity(
            id = 114,
            couplet = "दिल से निकली हुई दुआ और अपनों का साथ,\nइससे बड़ी कोई ख़ुशी नहीं मिलती ज़िंदगी में।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "आशीर्वाद"
        ),
        ShayariEntity(
            id = 115,
            couplet = "गुनगुनाते हुए जो सफ़र तय करते हैं,\nउन्हें काँटों भरे रास्ते भी फूलों से लगते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "उमंग"
        ),
        ShayariEntity(
            id = 116,
            couplet = "चमकती आँखों में जब नए सपने पलते हैं,\nतब हर दिन नई ख़ुशी का पैग़ाम लाता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.KHUSHI.id,
            moodTag = "पैग़ाम"
        ),

        // ==================== वक़्त व सुकून (WAQT) - 12 ====================
        ShayariEntity(
            id = 117,
            couplet = "वक़्त सब कुछ बदल देता है धीरे-धीरे,\nबस इंसान को थोड़ा सब्र रखना पड़ता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "वक़्त • सब्र"
        ),
        ShayariEntity(
            id = 118,
            couplet = "जो मिला है उसमें सुकून ढूँढना सीख लो,\nवरना ख़्वाहिशों की दौड़ तो कभी ख़त्म नहीं होती।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "सुकून • संतोष"
        ),
        ShayariEntity(
            id = 119,
            couplet = "गुज़रा हुआ वक़्त लौटकर नहीं आता कभी,\nइसलिए हर लम्हे को पूरे दिल से जिया करो।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "लम्हे • समय"
        ),
        ShayariEntity(
            id = 120,
            couplet = "सही वक़्त का इंतज़ार करने से बेहतर है,\nहर वक़्त को अपने काम से सही बना दिया जाए।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "कर्म • समय"
        ),
        ShayariEntity(
            id = 121,
            couplet = "सुकून किसी महल या दौलत में नहीं मिलता,\nये तो साफ़ दिल और सच्ची नीयत से मिलता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "सुकून • नीयत"
        ),
        ShayariEntity(
            id = 122,
            couplet = "धैर्य रखने वाले कभी खाली हाथ नहीं रहते,\nवक़्त आने पर उन्हें सबसे बेहतरीन मिलता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "धैर्य"
        ),
        ShayariEntity(
            id = 123,
            couplet = "तेज़ भागती दुनिया में थोड़ा ठहरना भी ज़रूरी है,\nताकि ख़ुद की आवाज़ और दिल की धड़कन सुन सकें।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "ठहराव"
        ),
        ShayariEntity(
            id = 124,
            couplet = "वक़्त से बड़ा कोई उस्ताद नहीं होता,\nये बिना किताबों के भी जीवन का हर पाठ पढ़ा देता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "उस्ताद"
        ),
        ShayariEntity(
            id = 125,
            couplet = "माफ़ करना सीख लेने से मन हल्का हो जाता है,\nदिल में बोझ रखकर कोई सुकून से सो नहीं पाता।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "क्षमा • शांति"
        ),
        ShayariEntity(
            id = 126,
            couplet = "बुरा वक़्त भी एक दिन गुज़र ही जाता है,\nजैसे काली रात के बाद उजाला ज़रूर आता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "परिवर्तन"
        ),
        ShayariEntity(
            id = 127,
            couplet = "कम शब्दों में बड़ी बात कह जाना ही कला है,\nऔर शोर के बीच शांत रहना ही असली ताक़त है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "शांति"
        ),
        ShayariEntity(
            id = 128,
            couplet = "आज जो बीज तुम मेहनत और ईमानदारी के बोओगे,\nआने वाला वक़्त उसी की मीठी फ़सल देगा।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.WAQT.id,
            moodTag = "ईमानदारी"
        ),

        // ==================== माँ-बाप व परिवार (PARIVAR) - 12 ====================
        ShayariEntity(
            id = 129,
            couplet = "किसी को घर मिला हिस्से में या कोई दुकाँ आई,\nमैं घर में सबसे छोटा था मेरे हिस्से में माँ आई।",
            poet = "मुनव्वर राना",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "माँ • परिवार"
        ),
        ShayariEntity(
            id = 130,
            couplet = "चलती फिरती हुई आँखों से अज़ाँ देखी है,\nमैंने जन्नत तो नहीं देखी है माँ देखी है।",
            poet = "मुनव्वर राना",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "माँ • जन्नत"
        ),
        ShayariEntity(
            id = 131,
            couplet = "पिता के साये में हर मुश्किल आसान लगती है,\nउनकी एक थपथपी से सारी थकान मिट जाती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "पिता • छाँव"
        ),
        ShayariEntity(
            id = 132,
            couplet = "परिवार वह वृक्ष है जिसकी जड़ें माता-पिता हैं,\nऔर उसकी हर शाखा पर अपनों का प्यार खिलता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "परिवार"
        ),
        ShayariEntity(
            id = 133,
            couplet = "मकान तो ईंट और पत्थर से बन जाते हैं,\nपर घर अपनों के प्यार और हँसी से बनता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "घर"
        ),
        ShayariEntity(
            id = 134,
            couplet = "माँ के क़दमों में ही सारी दुनिया की दौलत है,\nउनकी दुआओं से ही मेरी हर सुबह ख़ूबसूरत है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "दुआ • माँ"
        ),
        ShayariEntity(
            id = 135,
            couplet = "दुनिया की भीड़ में कोई कितना भी आगे निकल जाए,\nसच्चा सुकून तो माँ-बाप के पास बैठकर ही मिलता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "माँ-बाप"
        ),
        ShayariEntity(
            id = 136,
            couplet = "भाई-बहन की नोकझोंक में भी कितना प्यार छुपा होता है,\nबचपन का ये रिश्ता उम्र भर सबसे प्यारा होता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "भाई-बहन"
        ),
        ShayariEntity(
            id = 137,
            couplet = "बिना बोले ही जो दिल का हाल समझ ले,\nपूरी कायनात में वो सिर्फ़ माँ ही होती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "ममता"
        ),
        ShayariEntity(
            id = 138,
            couplet = "बड़ों का आशीर्वाद सिर पर हो तो डर कैसा,\nअपनों का साथ हो तो हर सफ़र सुहाना लगता है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "आशीर्वाद"
        ),
        ShayariEntity(
            id = 139,
            couplet = "ख़ुद धूप में जलकर भी जो हमें छाँव देते हैं,\nवो पिता ही हैं जो चुपचाप हर ख़्वाब पूरा करते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "त्याग • पिता"
        ),
        ShayariEntity(
            id = 140,
            couplet = "साथ बैठकर खाना और मिलकर मुस्कुराना,\nयही तो है एक खुशहाल परिवार का असली खज़ाना।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.PARIVAR.id,
            moodTag = "खज़ाना"
        ),

        // ==================== देशभक्ति (DESHPREM) - 12 ====================
        ShayariEntity(
            id = 141,
            couplet = "सारे जहाँ से अच्छा हिंदोस्ताँ हमारा,\nहम बुलबुलें हैं इसकी ये गुलसिताँ हमारा।",
            poet = "अल्लामा इक़बाल",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "वतन • गौरव"
        ),
        ShayariEntity(
            id = 142,
            couplet = "मज़हब नहीं सिखाता आपस में बैर रखना,\nहिंदी हैं हम वतन है हिंदोस्ताँ हमारा।",
            poet = "अल्लामा इक़बाल",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "एकता • देशभक्ति"
        ),
        ShayariEntity(
            id = 143,
            couplet = "सरफ़रोशी की तमन्ना अब हमारे दिल में है,\nदेखना है ज़ोर कितना बाज़ू-ए-क़ातिल में है।",
            poet = "बिस्मिल अज़ीमाबादी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "आज़ादी • जोश"
        ),
        ShayariEntity(
            id = 144,
            couplet = "वतन की मिट्टी से बढ़कर कोई चंदन नहीं होता,\nतिरंगे की शान से ऊँचा कोई बंधन नहीं होता।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "तिरंगा • वतन"
        ),
        ShayariEntity(
            id = 145,
            couplet = "रंग अनेक हैं यहाँ भाषाएं भी अनगिनत हैं,\nफिर भी हर दिल में धड़कता एक ही हिंदुस्तान है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "विविधता में एकता"
        ),
        ShayariEntity(
            id = 146,
            couplet = "जो वतन के काम आए वही ज़िंदगी अनमोल है,\nदेशप्रेम से बड़ा इस दुनिया में न कोई मोल है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "समर्पण"
        ),
        ShayariEntity(
            id = 147,
            couplet = "लहराता रहे यूँ ही आसमाँ में तिरंगा हमारा,\nहर दिल में बसता रहे ये प्यारा मुल्क हमारा।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "शान"
        ),
        ShayariEntity(
            id = 148,
            couplet = "वीराें की कुर्बानी से मिली है ये आज़ादी,\nहर साँस से निभाएंगे हम इसकी हिफ़ाज़त की ज़िम्मेदारी।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "शौर्य"
        ),
        ShayariEntity(
            id = 149,
            couplet = "गंगा की लहरें और हिमालय का ताज है,\nअपनी मातृभूमि पर हमें हर पल नाज़ है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "मातृभूमि"
        ),
        ShayariEntity(
            id = 150,
            couplet = "सपनों का नया भारत हम सबको मिलकर बनाना है,\nमेहनत और सच्चाई से देश को आगे बढ़ाना है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "प्रगति"
        ),
        ShayariEntity(
            id = 151,
            couplet = "सरहद पर जो जागते हैं उनकी बदौलत हम चैन से सोते हैं,\nसलाम है उन जवानों को जो वतन पर सब कुछ न्योछावर करते हैं।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "जवान • नमन"
        ),
        ShayariEntity(
            id = 152,
            couplet = "चाहे दुनिया के किसी भी कोने में चले जाएँ,\nवतन की मिट्टी की ख़ुशबू हमेशा दिल में रहती है।",
            poet = "_ शायरी",
            categoryId = ShayariCategory.DESHPREM.id,
            moodTag = "माटी की ख़ुशबू"
        )
    )

    private val baseShayari: List<ShayariEntity> by lazy {
        classicShayari +
            SeedBonusExisting.items +
            SeedRomantic.items +
            SeedBewafai.items +
            SeedSharabi.items +
            SeedKhudaRab.items +
            SeedMautJanaza.items +
            SeedSecretLove.items +
            SeedGenZ.items +
            SeedNibbaNibbi.items +
            SeedAttitude.items +
            SeedTareefAndAaina.tareefItems +
            SeedTareefAndAaina.aainaItems +
            SeedAansuAndMusafir.aansuItems +
            SeedAansuAndMusafir.musafirItems +
            SeedFestive.newYearItems +
            SeedFestive.christmasItems +
            SeedJijaSaliAndMulla.jijaSaliItems +
            SeedJijaSaliAndMulla.mullaItems +
            SeedSantaBantaAndMotuPatlu.santaBantaItems +
            SeedSantaBantaAndMotuPatlu.motuPatluItems
    }

    private fun loadExtraCatalog(): List<ShayariEntity> {
        val resourceNames = listOf("shayari_catalog_extra.tsv", "shayari_catalog_20k.tsv")
        val result = ArrayList<ShayariEntity>(28_700)
        val classLoader = ShayariSeedData::class.java.classLoader
        resourceNames.forEach { resourceName ->
            val stream = classLoader?.getResourceAsStream(resourceName) ?: return@forEach
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                reader.forEachLine { rawLine ->
                    if (rawLine.isBlank()) return@forEachLine
                    val parts = rawLine.split('\t', limit = 5)
                    if (parts.size == 5) {
                        val id = parts[0].toIntOrNull() ?: return@forEachLine
                        val categoryId = parts[1]
                        val poet = parts[2]
                        val moodTag = parts[3]
                        val couplet = parts[4].replace("\\n", "\n")
                        result.add(
                            ShayariEntity(
                                id = id,
                                couplet = couplet,
                                poet = poet,
                                categoryId = categoryId,
                                moodTag = moodTag,
                                isFeatured = false
                            )
                        )
                    }
                }
            }
        }
        return result
    }

    const val EXPECTED_CATALOG_SIZE = 30_000

    val allShayari: List<ShayariEntity> by lazy {
        baseShayari + loadExtraCatalog()
    }

    fun boldCategoryItems(): List<ShayariEntity> = SeedSecretLove.items

    fun boldCategorySample(): ShayariEntity = SeedSecretLove.items.first()
}
