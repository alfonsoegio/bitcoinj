/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.examples;


import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;

import javax.xml.bind.DatatypeConverter;

/**
 * SignTx on testnet ... does not work yet
 */
public class TxSigner {

    public static void main(String[] args) throws Exception {

        NetworkParameters params;
        params = TestNet3Params.get();

        // Initial data containing destination and unspent output address hashes
        String destinationPubKeyHash = "mrB5ZczDEMJtJ7xGXuD2tE2Qn4XX8RDRjX";
        String wif = "cRvcTPJtAxsYvn7Jnxw4BnymC7VfYF1HifVMh6uf7HbhsBXfyDqM";

        // Transaction containing the UTXO
        String originTxId = "8e213ee647d60de7bcb145cbf5f75527e892a459423b248c4ef610c2183b86b8";

        // Build the ECKey from the Wallet Interchange Format (wif) string
        DumpedPrivateKey dpk = DumpedPrivateKey.fromBase58(null, wif);
        ECKey key = dpk.getKey();
        String check = key.getPrivateKeyAsWiF(params);
        System.out.println(check);

        // Building addresses
        Address destinationAddress = Address.fromString(params, destinationPubKeyHash);

        // Initialize testnet transaction
        Transaction tx = new Transaction(params);

        // Add output spending the UTXO
        Long amount = new Long(8800000);
        tx.addOutput(Coin.valueOf(amount - 5000), destinationAddress);

        // Add some data in a second non-standard output
        String msg = "https://arxiv.org/abs/quant-ph/0012067";
        tx.addOutput(Coin.ZERO,
                ScriptBuilder.createOpReturnScript(msg.getBytes()));

        System.out.println(key.getPublicKeyAsHex());
        // Previous script
        ScriptBuilder redeemScriptBuilder = new ScriptBuilder();
        redeemScriptBuilder.op(ScriptOpCodes.OP_DUP)
                .op(ScriptOpCodes.OP_HASH160)
                .data(key.getPubKeyHash())
                .op(ScriptOpCodes.OP_EQUALVERIFY)
                .op(ScriptOpCodes.OP_CHECKSIG);
        Script redeemScript = redeemScriptBuilder.build();

        // Add originTxId as input with redeem script
        tx.addInput(Sha256Hash.wrap(originTxId),
                0,
                redeemScript);

        TransactionSignature txSignature = tx.calculateSignature(
                0,
                key,
                key.getPubKey(),
                Transaction.SigHash.ALL,
                false);

        Script scriptSig = new ScriptBuilder()
                .data(txSignature.encodeToBitcoin())
                .data(key.getPubKey())
                .build();
        assert (TransactionSignature.isEncodingCanonical(txSignature.encodeToBitcoin()));
        tx.getInput(0).setScriptSig(scriptSig);

        tx.verify();

        // Obtaining the hexadecimal raw transaction (does not validate)
        String tx_hex = DatatypeConverter.printHexBinary(tx.bitcoinSerialize());
        System.out.println(tx_hex);

    }

}
