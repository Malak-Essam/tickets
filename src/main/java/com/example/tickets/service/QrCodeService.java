package com.example.tickets.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tickets.domain.QrCode;
import com.example.tickets.domain.QrCodeStatusEnum;
import com.example.tickets.domain.Ticket;
import com.example.tickets.repository.QrCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private static final int QR_CODE_SIZE = 300;

    private final QrCodeRepository qrCodeRepository;

    @Transactional
    public QrCode generate(Ticket ticket) {
        UUID id = UUID.randomUUID();
        String encoded = Base64.getEncoder().encodeToString(toPng(id.toString()));
        QrCode qrCode = QrCode.builder()
            .id(id)
            .status(QrCodeStatusEnum.ACTIVE)
            .value(encoded)
            .ticket(ticket)
            .build();
        return qrCodeRepository.save(qrCode);
    }

    private byte[] toPng(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE,
                QR_CODE_SIZE, QR_CODE_SIZE);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException | WriterException e) {
            throw new IllegalStateException("Failed to encode QR code", e);
        }
    }
}