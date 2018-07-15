package com.samples.pdfrenderersample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PDFRenderer extends Fragment {

    // Nome do arquivo PDF
    private static final String FILENAME = "sample.pdf";

    // Descritor de arquivos do PDF
    private ParcelFileDescriptor mFileDescriptor;

    // Renderizador do PDF
    private PdfRenderer mPdfRenderer;

    // Página que está sendo exibida
    private PdfRenderer.Page mCurrentPage;

    // ImageView que exibirá as páginas do PDF como Bitmap
    private ImageView imageView;

    // Botões de navegação entre as páginas

    private Button previousButton;
    private Button nextButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdfrenderer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        imageView = (ImageView) view.findViewById(R.id.imageView);
        previousButton = (Button) view.findViewById(R.id.previousButton);
        nextButton = (Button) view.findViewById(R.id.nextButton);

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPage(mCurrentPage.getIndex() - 1);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPage(mCurrentPage.getIndex() + 1);
            }
        });

        // Exibe a primeira página como padrão
        int index = 0;
        showPage(index);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            openRenderer(activity);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            activity.finish();
        }
    }

    @Override
    public void onDetach() {
        try {
            closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDetach();
    }

    private void openRenderer(Context context) throws IOException {

        File file = new File(context.getCacheDir(), FILENAME);

        if (!file.exists()) {

            InputStream asset = context.getAssets().open(FILENAME);

            FileOutputStream output = new FileOutputStream(file);

            final byte[] buffer = new byte[1024];
            int size;

            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }

            asset.close();
            output.close();
        }
        mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        mPdfRenderer = new PdfRenderer(mFileDescriptor);
    }

    private void closeRenderer() throws IOException {

        if (null != mCurrentPage) {
            mCurrentPage.close();
        }

        mPdfRenderer.close();
        mFileDescriptor.close();
    }

    private void showPage(int index) {

        if (mPdfRenderer.getPageCount() <= index) {
            return;
        }

        // É necessário fechar a página atual antes de abrir outra

        if (null != mCurrentPage) {
            mCurrentPage.close();
        }

        // Uso do método 'openPage' para abrir uma página específica no arquivo

        mCurrentPage = mPdfRenderer.openPage(index);

        // Importante: O bitmap de destino deve ser ARGB (não RGB)

        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);

        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        imageView.setImageBitmap(bitmap);
        updateUI();
    }

    private void updateUI() {

        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();

        previousButton.setEnabled(0 != index);
        nextButton.setEnabled(index + 1 < pageCount);

        getActivity().setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
    }
}
